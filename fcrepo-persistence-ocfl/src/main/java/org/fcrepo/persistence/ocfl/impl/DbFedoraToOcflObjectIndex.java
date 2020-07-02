/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Maps Fedora IDs to the OCFL IDs of the OCFL objects the Fedora resource is stored in. This implementation is backed
 * by a relational database.
 */
@Component
public class DbFedoraToOcflObjectIndex implements FedoraToOcflObjectIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbFedoraToOcflObjectIndex.class);

    private static final String DDL = "sql/default-ocfl-index.sql";

    private static final String MAPPING_TABLE = "ocfl_id_map";

    private static final String FEDORA_ID_COLUMN = "fedora_id";

    private static final String FEDORA_ROOT_ID_COLUMN = "fedora_root_id";

    private static final String OCFL_ID_COLUMN = "ocfl_id";

    private static final String TRANSACTION_OPERATIONS_TABLE = "ocfl_id_map_session_operations";

    private static final String TRANSACTION_ID_COLUMN = "session_id";

    private static final String OPERATION_COLUMN = "operation";

    /*
     * Lookup all mappings for the resource id.
     */
    private static final String LOOKUP_MAPPING = "SELECT " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " FROM " +
            MAPPING_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId";

    /*
     * Lookup all mappings from the mapping table as well as any new 'add's and excluding any 'delete's in this
     * transaction.
     */
    private static final String LOOKUP_MAPPING_IN_TRANSACTION = "SELECT x." + FEDORA_ROOT_ID_COLUMN + "," +
            " x." + OCFL_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " FROM " + MAPPING_TABLE + " WHERE " +
            FEDORA_ID_COLUMN + " = :fedoraId" +
            " UNION SELECT " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId" +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete')";

    /*
     * Add an 'add' operation to the transaction table.
     */
    private static final String INSERT_MAPPING = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ")" +
            " VALUES (:fedoraId, :fedoraRootId, :ocflId, :transactionId, 'add')";

    /*
     * Is there an add operation for the same mapping in the same transaction?
     */
    private static final String IS_MAPPING_ADDED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    /*
     * Undo an add operation for the same mapping in the same transaction.
     */
    private static final String UNDO_ADD_MAPPING_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    /*
     * Add a delete operation to the transaction table.
     */
    private static final String REMOVE_MAPPING = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ")" +
            " VALUES (:fedoraId, :transactionId, 'delete')";

    /*
     * Add records to the mapping table that are to be added in this transaction.
     */
    private static final String COMMIT_ADD_RECORDS = "INSERT INTO " + MAPPING_TABLE + " ( " + FEDORA_ID_COLUMN + ", "
            + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " ) SELECT " + FEDORA_ID_COLUMN + ", " +
            FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    /*
     * Delete records from the mapping table that are to be deleted in this transaction.
     */
    private static final String COMMIT_DELETE_RECORDS = "DELETE FROM " + MAPPING_TABLE + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " +  OPERATION_COLUMN + " = 'delete' AND " +
            MAPPING_TABLE + "." + FEDORA_ID_COLUMN + " = " + TRANSACTION_OPERATIONS_TABLE + "." + FEDORA_ID_COLUMN +
            ")";

    /*
     * Is there a mapping 'delete'd in this session?
     */
    private static final String IS_MAPPING_REMOVED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId AND " + FEDORA_ROOT_ID_COLUMN + " = :fedoraRootId AND " +
            OCFL_ID_COLUMN + " = :ocflId AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN +
            " = 'delete'";

    /*
     * Delete the mapping from the session table.
     */
    private static final String UNDO_REMOVE_MAPPING_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId AND " + FEDORA_ROOT_ID_COLUMN + " = :fedoraRootId AND " +
            OCFL_ID_COLUMN + " = :ocflId AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN +
            " = 'delete'";


    private static final String TRUNCATE_MAPPINGS = "TRUNCATE TABLE " + MAPPING_TABLE;

    private static final String TRUNCATE_TRANSACTIONS = "TRUNCATE TABLE " + TRANSACTION_OPERATIONS_TABLE;

    /*
     * Delete all records from the transaction table for the specified transaction.
     */
    private static final String DELETE_ENTIRE_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId";

    /*
     * Row mapper for the Lookup queries.
     */
    private static final RowMapper<FedoraOCFLMapping> GET_MAPPING_ROW_MAPPER = (resultSet, i) -> new FedoraOCFLMapping(
            resultSet.getString(1),
            resultSet.getString(2)
    );

    private final DataSource dataSource;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private PlatformTransactionManager platformTransactionManager;

    public DbFedoraToOcflObjectIndex(@Autowired final DataSource dataSource) {
        this.dataSource = dataSource;
        this.platformTransactionManager = new DataSourceTransactionManager(dataSource);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @PostConstruct
    public void setup() {
        LOGGER.info("Applying ddl: {}", DDL);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + DDL)),
                dataSource);
    }

    @Override
    public FedoraOCFLMapping getMapping(final String transactionId, final String fedoraId)
            throws FedoraOCFLMappingNotFoundException {
        try {
            final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("fedoraId", fedoraId);
            if (transactionId != null) {
                parameterSource.addValue("transactionId", transactionId);
                return jdbcTemplate.queryForObject(LOOKUP_MAPPING_IN_TRANSACTION, parameterSource,
                        GET_MAPPING_ROW_MAPPER);
            } else {
                return jdbcTemplate.queryForObject(LOOKUP_MAPPING, parameterSource, GET_MAPPING_ROW_MAPPER);
            }
        } catch (final EmptyResultDataAccessException e) {
            throw new FedoraOCFLMappingNotFoundException("No OCFL mapping found for " + fedoraId);
        }
    }

    @Override
    public FedoraOCFLMapping addMapping(@Nonnull final String transactionId, final String fedoraId,
                                        final String fedoraRootId, final String ocflId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("fedoraId", fedoraId);
        parameterSource.addValue("fedoraRootId", fedoraRootId);
        parameterSource.addValue("ocflId", ocflId);
        parameterSource.addValue("transactionId", transactionId);
        final boolean isRemovedInTx = !jdbcTemplate.queryForList(IS_MAPPING_REMOVED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (isRemovedInTx) {
            jdbcTemplate.update(UNDO_REMOVE_MAPPING_IN_TRANSACTION, parameterSource);
        } else {
            jdbcTemplate.update(INSERT_MAPPING, parameterSource);
        }
        return new FedoraOCFLMapping(fedoraRootId, ocflId);
    }

    @Override
    public void removeMapping(@Nonnull final String transactionId, final String fedoraId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("fedoraId", fedoraId);
        parameterSource.addValue("transactionId", transactionId);
        final boolean isAddedInTx = !jdbcTemplate.queryForList(IS_MAPPING_ADDED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (isAddedInTx) {
            jdbcTemplate.update(UNDO_ADD_MAPPING_IN_TRANSACTION, parameterSource);
        } else {
            jdbcTemplate.update(REMOVE_MAPPING, parameterSource);
        }
    }

    @Override
    public void reset() {
        final String tempSessionId = UUID.randomUUID().toString();
        executeInDbTransaction(tempSessionId, status -> {
            try {
                jdbcTemplate.update(TRUNCATE_MAPPINGS, Collections.emptyMap());
                jdbcTemplate.update(TRUNCATE_TRANSACTIONS, Collections.emptyMap());
                return null;
            } catch (final Exception e) {
                status.setRollbackOnly();
                throw new RepositoryRuntimeException("Failed to truncate FedoraToOcfl index tables", e);
            }
        });
    }

    @Override
    public void commit(@Nonnull final String sessionId) {
        final Map<String, String> map = Map.of("transactionId", sessionId);
        executeInDbTransaction(sessionId, status -> {
            try {
                jdbcTemplate.update(COMMIT_DELETE_RECORDS, map);
                jdbcTemplate.update(COMMIT_ADD_RECORDS, map);
                jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, map);
                return null;
            } catch (final Exception e) {
                status.setRollbackOnly();
                LOGGER.warn("Unable to commit FedoraToOcfl index transaction {}: {}", sessionId, e.getMessage());
                throw new RepositoryRuntimeException("Unable to commit FedoraToOcfl index transaction", e);
            }
        });
    }

    @Override
    public void rollback(@Nonnull final String sessionId) {
        jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, Map.of("transactionId", sessionId));
    }

    private <T> void executeInDbTransaction(final String txId, final TransactionCallback<T> callback) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        // Seemingly setting the name ensures that we don't re-use a transaction.
        transactionTemplate.setName("tx-" + txId);
        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
        transactionTemplate.execute(callback);
    }
}
