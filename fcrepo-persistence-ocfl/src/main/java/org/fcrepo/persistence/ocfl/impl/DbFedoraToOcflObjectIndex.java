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

import com.google.common.base.Preconditions;
import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

/**
 * Maps Fedora IDs to the OCFL IDs of the OCFL objects the Fedora resource is stored in. This implementation is backed
 * by a relational database.
 *
 * @author pwinckles
 */
@Component("ocflIndexImpl")
public class DbFedoraToOcflObjectIndex implements FedoraToOcflObjectIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbFedoraToOcflObjectIndex.class);

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(
            DbPlatform.MYSQL, "sql/mysql-ocfl-index.sql",
            DbPlatform.H2, "sql/default-ocfl-index.sql",
            DbPlatform.POSTGRESQL, "sql/default-ocfl-index.sql",
            DbPlatform.MARIADB, "sql/default-ocfl-index.sql"
    );

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
            " AND " + OPERATION_COLUMN + " = 'add') x";

    /*
     * Add an 'add' operation to the transaction table.
     */
    private static final String UPSERT_MAPPING_TX_POSTGRESQL = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ") VALUES (:fedoraId, :fedoraRootId, :ocflId," +
            " :transactionId, :operation) ON CONFLICT (" + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ")" +
            " DO UPDATE SET " + FEDORA_ROOT_ID_COLUMN + " = EXCLUDED." + FEDORA_ROOT_ID_COLUMN + ", " +
            OCFL_ID_COLUMN + " = EXCLUDED." + OCFL_ID_COLUMN + ", " + OPERATION_COLUMN + " = EXCLUDED." +
            OPERATION_COLUMN;

    private static final String UPSERT_MAPPING_TX_MYSQL_MARIA = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ")" +
            " VALUES (:fedoraId, :fedoraRootId, :ocflId, :transactionId, :operation) ON DUPLICATE KEY UPDATE " +
            FEDORA_ROOT_ID_COLUMN + " = VALUES(" + FEDORA_ROOT_ID_COLUMN + "), " + OCFL_ID_COLUMN + " = VALUES(" +
            OCFL_ID_COLUMN + "), " + OPERATION_COLUMN + " = VALUES(" + OPERATION_COLUMN + ")";

    private static final String UPSERT_MAPPING_TX_H2 = "MERGE INTO " + TRANSACTION_OPERATIONS_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ")" +
            " KEY (" + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ")" +
            " VALUES (:fedoraId, :fedoraRootId, :ocflId, :transactionId, :operation)";

    /**
     * Map of database product to UPSERT into operations table SQL.
     */
    private static final Map<DbPlatform, String> UPSERT_MAPPING_TX_MAP = Map.of(
            DbPlatform.MYSQL, UPSERT_MAPPING_TX_MYSQL_MARIA,
            DbPlatform.H2, UPSERT_MAPPING_TX_H2,
            DbPlatform.POSTGRESQL, UPSERT_MAPPING_TX_POSTGRESQL,
            DbPlatform.MARIADB, UPSERT_MAPPING_TX_MYSQL_MARIA
    );

    private static final String COMMIT_ADD_MAPPING_POSTGRESQL = "INSERT INTO " + MAPPING_TABLE +
            " ( " + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ") SELECT " +
            FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + OPERATION_COLUMN + " = 'add' AND " + TRANSACTION_ID_COLUMN +
            " = :transactionId ON CONFLICT ( " +  FEDORA_ID_COLUMN + " )" +
            " DO UPDATE SET " + FEDORA_ROOT_ID_COLUMN + " = EXCLUDED." + FEDORA_ROOT_ID_COLUMN + ", " +
            OCFL_ID_COLUMN + " = EXCLUDED." + OCFL_ID_COLUMN;

    private static final String COMMIT_ADD_MAPPING_MYSQL_MARIA = "INSERT INTO " + MAPPING_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ") SELECT " +
            FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + OPERATION_COLUMN + " = 'add' AND " + TRANSACTION_ID_COLUMN +
            " = :transactionId ON DUPLICATE KEY UPDATE " +
            FEDORA_ROOT_ID_COLUMN + " = VALUES(" + FEDORA_ROOT_ID_COLUMN + "), " + OCFL_ID_COLUMN + " = VALUES(" +
            OCFL_ID_COLUMN + ")";

    private static final String COMMIT_ADD_MAPPING_H2 = "MERGE INTO " + MAPPING_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ")" +
            " SELECT " + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + OPERATION_COLUMN + " = 'add'";

    /**
     * Map of database product name to COMMIT to mapping table from operations table
     */
    private static final Map<DbPlatform, String> COMMIT_ADD_MAPPING_MAP = Map.of(
            DbPlatform.MYSQL, COMMIT_ADD_MAPPING_MYSQL_MARIA,
            DbPlatform.H2, COMMIT_ADD_MAPPING_H2,
            DbPlatform.POSTGRESQL, COMMIT_ADD_MAPPING_POSTGRESQL,
            DbPlatform.MARIADB, COMMIT_ADD_MAPPING_MYSQL_MARIA
    );

    /*
     * Delete records from the mapping table that are to be deleted in this transaction.
     */
    private static final String COMMIT_DELETE_RECORDS = "DELETE FROM " + MAPPING_TABLE + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " +  OPERATION_COLUMN + " = 'delete' AND " +
            MAPPING_TABLE + "." + FEDORA_ID_COLUMN + " = " + TRANSACTION_OPERATIONS_TABLE + "." + FEDORA_ID_COLUMN +
            ")";

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
    private static final RowMapper<FedoraOcflMapping> GET_MAPPING_ROW_MAPPER = (resultSet, i) -> new FedoraOcflMapping(
            FedoraId.create(resultSet.getString(1)),
            resultSet.getString(2)
    );

    private final DataSource dataSource;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private DbPlatform dbPlatform;

    public DbFedoraToOcflObjectIndex(@Autowired final DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @PostConstruct
    public void setup() {
        dbPlatform = DbPlatform.fromDataSource(dataSource);

        Preconditions.checkArgument(UPSERT_MAPPING_TX_MAP.containsKey(dbPlatform),
                "Missing SQL mapping for %s", dbPlatform);
        Preconditions.checkArgument(COMMIT_ADD_MAPPING_MAP.containsKey(dbPlatform),
                "Missing SQL mapping for %s", dbPlatform);
        Preconditions.checkArgument(DDL_MAP.containsKey(dbPlatform),
                "Missing DDL mapping for %s", dbPlatform);

        final var ddl = DDL_MAP.get(dbPlatform);
        LOGGER.info("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                dataSource);
    }

    @Override
    public FedoraOcflMapping getMapping(final String transactionId, final FedoraId fedoraId)
            throws FedoraOcflMappingNotFoundException {
        try {
            final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("fedoraId", fedoraId.getResourceId());
            if (transactionId != null) {
                parameterSource.addValue("transactionId", transactionId);
                return jdbcTemplate.queryForObject(LOOKUP_MAPPING_IN_TRANSACTION, parameterSource,
                        GET_MAPPING_ROW_MAPPER);
            } else {
                return jdbcTemplate.queryForObject(LOOKUP_MAPPING, parameterSource, GET_MAPPING_ROW_MAPPER);
            }
        } catch (final EmptyResultDataAccessException e) {
            throw new FedoraOcflMappingNotFoundException("No OCFL mapping found for " + fedoraId);
        }
    }

    @Override
    public FedoraOcflMapping addMapping(@Nonnull final String transactionId, final FedoraId fedoraId,
                                        final FedoraId fedoraRootId, final String ocflId) {
        upsert(transactionId, fedoraId, "add", fedoraRootId, ocflId);
        return new FedoraOcflMapping(fedoraRootId, ocflId);
    }

    @Override
    public void removeMapping(@Nonnull final String transactionId, final FedoraId fedoraId) {
        upsert(transactionId, fedoraId, "delete");
    }

    private void upsert(final String transactionId, final FedoraId fedoraId, final String operation) {
        upsert(transactionId, fedoraId, operation, null, null);
    }

    /**
     * Perform the upsert to the operations table.
     *
     * @param transactionId the transaction/session id.
     * @param fedoraId the resource id.
     * @param operation the operation we are performing (add or delete)
     * @param fedoraRootId the fedora root id (for add only)
     * @param ocflId the ocfl id (for add only).
     */
    private void upsert(final String transactionId, final FedoraId fedoraId, final String operation,
                        final FedoraId fedoraRootId, final String ocflId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("fedoraId", fedoraId.getResourceId());
        parameterSource.addValue("fedoraRootId", fedoraRootId == null ? null : fedoraRootId.getResourceId());
        parameterSource.addValue("ocflId", ocflId);
        parameterSource.addValue("transactionId", transactionId);
        parameterSource.addValue("operation", operation);
        try {
            jdbcTemplate.update(UPSERT_MAPPING_TX_MAP.get(dbPlatform), parameterSource);
        } catch (final DataIntegrityViolationException | BadSqlGrammarException e) {
            if (e.getMessage().contains("too long for")) {
                throw new InvalidResourceIdentifierException("Database error - Fedora ID path too long",e);
            } else {
                throw new RepositoryRuntimeException("Database error - error during upsert",e);
            }
        }
    }

    @Transactional
    @Override
    public void reset() {
        try {
            jdbcTemplate.update(TRUNCATE_MAPPINGS, Collections.emptyMap());
            jdbcTemplate.update(TRUNCATE_TRANSACTIONS, Collections.emptyMap());
        } catch (final Exception e) {
            throw new RepositoryRuntimeException("Failed to truncate FedoraToOcfl index tables", e);
        }
    }

    @Transactional
    @Override
    public void commit(@Nonnull final String sessionId) {
        LOGGER.debug("Committing FedoraToOcfl index changes from transaction {}", sessionId);
        final Map<String, String> map = Map.of("transactionId", sessionId);
        try {
            jdbcTemplate.update(COMMIT_DELETE_RECORDS, map);
            jdbcTemplate.update(COMMIT_ADD_MAPPING_MAP.get(dbPlatform), map);
            jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, map);
        } catch (final Exception e) {
            LOGGER.warn("Unable to commit FedoraToOcfl index transaction {}: {}", sessionId, e.getMessage());
            throw new RepositoryRuntimeException("Unable to commit FedoraToOcfl index transaction", e);
        }
    }

    @Override
    public void rollback(@Nonnull final String sessionId) {
        jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, Map.of("transactionId", sessionId));
    }

}
