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
package org.fcrepo.kernel.impl;

import com.google.common.base.Preconditions;
import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.slf4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author peichman
 * @author whikloj
 * @since 6.0.0
 */
@Component("containmentIndexImpl")
public class ContainmentIndexImpl implements ContainmentIndex {

    private static final Logger LOGGER = getLogger(ContainmentIndexImpl.class);

    private int containsLimit = 50000;

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private DbPlatform dbPlatform;

    public static final String RESOURCES_TABLE = "containment";

    private static final String TRANSACTION_OPERATIONS_TABLE = "containment_transactions";

    public static final String FEDORA_ID_COLUMN = "fedora_id";

    private static final String PARENT_COLUMN = "parent";

    private static final String TRANSACTION_ID_COLUMN = "transaction_id";

    private static final String OPERATION_COLUMN = "operation";

    private static final String START_TIME_COLUMN = "start_time";

    private static final String END_TIME_COLUMN = "end_time";

    private static final String UPDATED_COLUMN = "updated";

    /*
     * Select children of a resource that are not marked as deleted.
     */
    private static final String SELECT_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent AND " + END_TIME_COLUMN + " IS NULL" +
            " ORDER BY " + FEDORA_ID_COLUMN + " LIMIT :containsLimit OFFSET :offSet";

    /*
     * Select children of a memento of a resource.
     */
    private static final String SELECT_CHILDREN_OF_MEMENTO = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent AND " + START_TIME_COLUMN +
            " <= :asOfTime AND (" + END_TIME_COLUMN + " > :asOfTime OR " + END_TIME_COLUMN + " IS NULL) ORDER BY " +
            FEDORA_ID_COLUMN + " LIMIT :containsLimit OFFSET :offSet";

    /*
     * Select children of a parent from resources table and from the transaction table with an 'add' operation,
     * but exclude any records that also exist in the transaction table with a 'delete' or 'purge' operation.
     */
    private static final String SELECT_CHILDREN_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent" +
            " AND " + END_TIME_COLUMN + " IS NULL " +
            " UNION SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = x." + FEDORA_ID_COLUMN +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " IN ('delete', 'purge'))" +
            " ORDER BY x." + FEDORA_ID_COLUMN + " LIMIT :containsLimit OFFSET :offSet";

    /*
     * Select all children of a resource that are marked for deletion.
     */
    private static final String SELECT_DELETED_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent AND " + END_TIME_COLUMN +
            " IS NOT NULL ORDER BY " + FEDORA_ID_COLUMN + " LIMIT :containsLimit OFFSET :offSet";

    /*
     * Select children of a resource plus children 'delete'd in the non-committed transaction, but excluding any
     * 'add'ed in the non-committed transaction.
     */
    private static final String SELECT_DELETED_CHILDREN_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN +
            " FROM (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + END_TIME_COLUMN + " IS NOT NULL UNION" +
            " SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            PARENT_COLUMN + " = :parent AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'delete') x" +
            " WHERE NOT EXISTS " +
            "(SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + PARENT_COLUMN + " = :parent AND " +
            FEDORA_ID_COLUMN + " = x." + FEDORA_ID_COLUMN + " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add') ORDER BY x." + FEDORA_ID_COLUMN + " LIMIT :containsLimit OFFSET :offSet";

    /*
     * Insert a parent child relationship to the transaction operation table.
     */
    private static final String INSERT_CHILD_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + " ) VALUES (:parent, :child, :startTime, :endTime, " +
            ":transactionId, 'add')";

    /*
     * Remove an insert row from the transaction operation table for this parent child relationship.
     */
    private static final String UNDO_INSERT_CHILD_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    /*
     * Add a parent child relationship deletion to the transaction operation table.
     */
    private static final String DELETE_CHILD_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + END_TIME_COLUMN + ", " + TRANSACTION_ID_COLUMN +
            ", " + OPERATION_COLUMN + " ) VALUES (:parent, :child, :endTime, :transactionId, 'delete')";

    /*
     * Add a parent child relationship purge to the transaction operation table.
     */
    private static final String PURGE_CHILD_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN +
            " ) VALUES (:parent, :child, :transactionId, 'purge')";

    /*
     * Remove a mark as deleted row from the transaction operation table for this child relationship (no parent).
     */
    private static final String UNDO_DELETE_CHILD_IN_TRANSACTION_NO_PARENT = "DELETE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    /*
     * Remove a purge row from the transaction operation table for this parent child relationship.
     */
    private static final String UNDO_PURGE_CHILD_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'purge'";

    /*
     * Is this parent child relationship being added in this transaction?
     */
    private static final String IS_CHILD_ADDED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + PARENT_COLUMN + " = :parent" +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    /*
     * Is this child's relationship being marked for deletion in this transaction (no parent)?
     */
    private static final String IS_CHILD_DELETED_IN_TRANSACTION_NO_PARENT = "SELECT TRUE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child " +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    /*
     * Is this parent child relationship being purged in this transaction?
     */
    private static final String IS_CHILD_PURGED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + PARENT_COLUMN + " = :parent" +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'purge'";

   /*
    * Delete all rows from the transaction operation table for this transaction.
    */
    private static final String DELETE_ENTIRE_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId";

    /*
     * Add to the main table all rows from the transaction operation table marked 'add' for this transaction.
     */
    private static final String COMMIT_ADD_RECORDS = "INSERT INTO " + RESOURCES_TABLE + " ( " + FEDORA_ID_COLUMN + ", "
            + PARENT_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + " ) SELECT " + FEDORA_ID_COLUMN +
            ", " + PARENT_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    /*
     * Add an end time to the rows in the main table that match all rows from transaction operation table marked
     * 'delete' for this transaction.
     */
    private static final String COMMIT_DELETE_RECORDS_H2 = "UPDATE " + RESOURCES_TABLE +
            " r SET r." + END_TIME_COLUMN + " = ( SELECT t." + END_TIME_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " t " +
            " WHERE t." + FEDORA_ID_COLUMN + " = r." + FEDORA_ID_COLUMN + " AND t." + TRANSACTION_ID_COLUMN +
            " = :transactionId AND t." +  OPERATION_COLUMN +
            " = 'delete' AND t." + PARENT_COLUMN + " = r." + PARENT_COLUMN + " AND r." +
            END_TIME_COLUMN + " IS NULL)" +
            " WHERE EXISTS (SELECT * FROM " + TRANSACTION_OPERATIONS_TABLE + " t WHERE t." + FEDORA_ID_COLUMN +
            " = r." + FEDORA_ID_COLUMN + " AND t." + TRANSACTION_ID_COLUMN + " = :transactionId AND t." +
            OPERATION_COLUMN + " = 'delete' AND t." + PARENT_COLUMN + " = r." + PARENT_COLUMN + " AND r." +
            END_TIME_COLUMN + " IS NULL)";

    private static final String COMMIT_DELETE_RECORDS_MYSQL = "UPDATE " + RESOURCES_TABLE +
            " r INNER JOIN " + TRANSACTION_OPERATIONS_TABLE + " t ON t." + FEDORA_ID_COLUMN + " = r." +
            FEDORA_ID_COLUMN + " SET r." + END_TIME_COLUMN + " = t." + END_TIME_COLUMN +
            " WHERE t." + PARENT_COLUMN + " = r." +
            PARENT_COLUMN + " AND t." + TRANSACTION_ID_COLUMN + " = :transactionId AND t." +  OPERATION_COLUMN +
            " = 'delete' AND r." + END_TIME_COLUMN + " IS NULL";

    private static final String COMMIT_DELETE_RECORDS_POSTGRES = "UPDATE " + RESOURCES_TABLE + " SET " +
            END_TIME_COLUMN + " = t." + END_TIME_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE + " t WHERE t." +
            FEDORA_ID_COLUMN + " = " + RESOURCES_TABLE + "." + FEDORA_ID_COLUMN + " AND t." + PARENT_COLUMN +
            " = " + RESOURCES_TABLE + "." + PARENT_COLUMN + " AND t." + TRANSACTION_ID_COLUMN +
            " = :transactionId AND t." + OPERATION_COLUMN + " = 'delete' AND " + RESOURCES_TABLE + "." +
            END_TIME_COLUMN + " IS NULL";

    private Map<DbPlatform, String> COMMIT_DELETE_RECORDS = Map.of(
            DbPlatform.H2, COMMIT_DELETE_RECORDS_H2,
            DbPlatform.MARIADB, COMMIT_DELETE_RECORDS_MYSQL,
            DbPlatform.MYSQL, COMMIT_DELETE_RECORDS_MYSQL,
            DbPlatform.POSTGRESQL, COMMIT_DELETE_RECORDS_POSTGRES
    );

    /*
     * Remove from the main table all rows from transaction operation table marked 'purge' for this transaction.
     */
    private static final String COMMIT_PURGE_RECORDS = "DELETE FROM " + RESOURCES_TABLE + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_OPERATIONS_TABLE + " t WHERE t." +
            TRANSACTION_ID_COLUMN + " = :transactionId AND t." +  OPERATION_COLUMN + " = 'purge' AND" +
            " t." + FEDORA_ID_COLUMN + " = " + RESOURCES_TABLE + "." + FEDORA_ID_COLUMN +
            " AND t." + PARENT_COLUMN + " = " + RESOURCES_TABLE + "." + PARENT_COLUMN + ")";

    /*
     * Query if a resource exists in the main table and is not deleted.
     */
    private static final String RESOURCE_EXISTS = "SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + END_TIME_COLUMN + " IS NULL";

    /*
     * Resource exists as a record in the transaction operations table with an 'add' operation and not also
     * exists as a 'delete' operation.
     */
    private static final String RESOURCE_EXISTS_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            "  AND " + END_TIME_COLUMN + " IS NULL UNION SELECT " + FEDORA_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN +
            " = :transactionId" + " AND " + OPERATION_COLUMN + " = 'add') x WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " IN ('delete', 'purge'))";

    /*
     * Query if a resource exists in the main table even if it is deleted.
     */
    private static final String RESOURCE_OR_TOMBSTONE_EXISTS = "SELECT " + FEDORA_ID_COLUMN + " FROM " +
            RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child";

    /*
     * Resource exists as a record in the main table even if deleted or in the transaction operations table with an
     * 'add' operation and not also exists as a 'delete' operation.
     */
    private static final String RESOURCE_OR_TOMBSTONE_EXISTS_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            " UNION SELECT " + FEDORA_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN +
            " = :transactionId" + " AND " + OPERATION_COLUMN + " = 'add') x WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " IN ('delete', 'purge'))";


    /*
     * Get the parent ID for this resource from the main table if not deleted.
     */
    private static final String PARENT_EXISTS = "SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + END_TIME_COLUMN + " IS NULL";

    /*
     * Get the parent ID for this resource from the operations table for an 'add' operation in this transaction, but
     * exclude any 'delete' operations for this resource in this transaction.
     */
    private static final String PARENT_EXISTS_IN_TRANSACTION = "SELECT x." + PARENT_COLUMN + " FROM" +
            " (SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            " AND " + END_TIME_COLUMN + " IS NULL" +
            " UNION SELECT " + PARENT_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'delete')";

    /*
     * Get the parent ID for this resource from the main table if deleted.
     */
    private static final String PARENT_EXISTS_DELETED = "SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + END_TIME_COLUMN + " IS NOT NULL";

    /*
     * Get the parent ID for this resource from main table and the operations table for a 'delete' operation in this
     * transaction, excluding any 'add' operations for this resource in this transaction.
     */
    private static final String PARENT_EXISTS_DELETED_IN_TRANSACTION = "SELECT x." + PARENT_COLUMN + " FROM" +
            " (SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            " AND " + END_TIME_COLUMN + " IS NOT NULL UNION SELECT " + PARENT_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN +
            " = :transactionId AND " + OPERATION_COLUMN + " = 'delete') x WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add')";

    /*
     * Does this resource exist in the transaction operation table for an 'add' record.
     */
    private static final String IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT = "SELECT TRUE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    /*
     * Delete a row from the transaction operation table with this resource and 'add' operation, no parent required.
     */
    private static final String UNDO_INSERT_CHILD_IN_TRANSACTION_NO_PARENT = "DELETE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String TRUNCATE_TABLE = "TRUNCATE TABLE ";

    /*
     * Any record tracked in the containment index is either active or a tombstone. Either way it exists for the
     * purpose of finding ghost nodes.
     */
    private static final String SELECT_ID_LIKE = "SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " +
            FEDORA_ID_COLUMN + " LIKE :resourceId";

    private static final String SELECT_ID_LIKE_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN + " FROM (SELECT " +
            FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " LIKE :resourceId" +
            " UNION SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            FEDORA_ID_COLUMN + " LIKE :resourceId AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add') x WHERE NOT EXISTS (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " LIKE :resourceId AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'delete')";

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(
            DbPlatform.MYSQL, "sql/mysql-containment.sql",
            DbPlatform.H2, "sql/default-containment.sql",
            DbPlatform.POSTGRESQL, "sql/postgres-containment.sql",
            DbPlatform.MARIADB, "sql/default-containment.sql"
    );

    private static final String SELECT_LAST_UPDATED = "SELECT " + UPDATED_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :resourceId";

    private static final String UPDATE_LAST_UPDATED = "UPDATE " + RESOURCES_TABLE + " SET " + UPDATED_COLUMN +
            " = :updated WHERE " + FEDORA_ID_COLUMN + " = :resourceId";

    private static final String SELECT_LAST_UPDATED_IN_TX = "SELECT MAX(x.updated)" +
            " FROM (SELECT " + UPDATED_COLUMN + " as updated FROM " + RESOURCES_TABLE + " WHERE " +
            FEDORA_ID_COLUMN + " = :resourceId UNION SELECT " + START_TIME_COLUMN +
            " as updated FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + PARENT_COLUMN + " = :resourceId AND " +
            OPERATION_COLUMN + " = 'add' AND " + TRANSACTION_ID_COLUMN + " = :transactionId UNION SELECT " +
            END_TIME_COLUMN + " as updated FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + PARENT_COLUMN +
            " = :resourceId AND " + OPERATION_COLUMN + " = 'delete' AND " + TRANSACTION_ID_COLUMN +
            " = :transactionId UNION SELECT " + END_TIME_COLUMN +
            " as updated FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + PARENT_COLUMN + " = :resourceId AND " +
            OPERATION_COLUMN + " = 'add' AND " + TRANSACTION_ID_COLUMN + " = :transactionId) x";

    private static final String GET_UPDATED_RESOURCES = "SELECT DISTINCT " + PARENT_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " in ('add', 'delete')";

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() {
        jdbcTemplate = getNamedParameterJdbcTemplate();

        dbPlatform = DbPlatform.fromDataSource(dataSource);

        Preconditions.checkArgument(DDL_MAP.containsKey(dbPlatform),
                "Missing DDL mapping for %s", dbPlatform);

        final var ddl = DDL_MAP.get(dbPlatform);
        LOGGER.info("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                dataSource);
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(getDataSource());
    }

    void setContainsLimit(final int limit) {
        containsLimit = limit;
    }

    @Override
    public Stream<String> getContains(final String txId, final FedoraId fedoraId) {
        final String resourceId = fedoraId.isMemento() ? fedoraId.getBaseId() : fedoraId.getFullId();
        final Instant asOfTime = fedoraId.isMemento() ? fedoraId.getMementoInstant() : null;
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", resourceId);

        LOGGER.debug("getContains for {} in transaction {} and instant {}", resourceId, txId, asOfTime);

        final String query;
        if (asOfTime == null) {
            if (txId != null) {
                // we are in a transaction
                parameterSource.addValue("transactionId", txId);
                query = SELECT_CHILDREN_IN_TRANSACTION;
            } else {
                // not in a transaction
                query = SELECT_CHILDREN;
            }
        } else {
            parameterSource.addValue("asOfTime", formatInstant(asOfTime));
            query = SELECT_CHILDREN_OF_MEMENTO;
        }

        return StreamSupport.stream(new ContainmentIterator(query, parameterSource), false);
    }

    @Override
    public Stream<String> getContainsDeleted(final String txId, final FedoraId fedoraId) {
        final String resourceId = fedoraId.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", resourceId);

        final String query;
        if (txId != null) {
            // we are in a transaction
            parameterSource.addValue("transactionId", txId);
            query = SELECT_DELETED_CHILDREN_IN_TRANSACTION;
        } else {
            // not in a transaction
            query = SELECT_DELETED_CHILDREN;
        }
        LOGGER.debug("getContainsDeleted for {} in transaction {}", resourceId, txId);
        return StreamSupport.stream(new ContainmentIterator(query, parameterSource), false);
    }

    @Override
    public String getContainedBy(final String txId, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final List<String> parentID;
        if (txId != null) {
            parameterSource.addValue("transactionId", txId);
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_IN_TRANSACTION, parameterSource, String.class);
        } else {
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS, parameterSource, String.class);
        }
        return parentID.stream().findFirst().orElse(null);
    }

    @Override
    public void addContainedBy(@Nonnull final String txId, final FedoraId parent, final FedoraId child) {
        addContainedBy(txId, parent, child, Instant.now(), null);
    }

    @Override
    public void addContainedBy(@Nonnull final String txId, final FedoraId parent, final FedoraId child,
                               final Instant startTime, final Instant endTime) {
        final String parentID = parent.getFullId();
        final String childID = child.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        LOGGER.debug("Adding: parent: {}, child: {}, in txn: {}, start time {}, end time {}", parentID, childID, txId,
                formatInstant(startTime), formatInstant(endTime));

        parameterSource.addValue("parent", parentID);
        parameterSource.addValue("child", childID);
        parameterSource.addValue("transactionId", txId);
        parameterSource.addValue("startTime", formatInstant(startTime));
        parameterSource.addValue("endTime", formatInstant(endTime));
        final boolean purgedInTxn = !jdbcTemplate.queryForList(IS_CHILD_PURGED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (purgedInTxn) {
            // We purged it, but are re-adding it so remove the purge operation.
            jdbcTemplate.update(UNDO_PURGE_CHILD_IN_TRANSACTION, parameterSource);
        }
        jdbcTemplate.update(INSERT_CHILD_IN_TRANSACTION, parameterSource);
    }

    @Override
    public void removeContainedBy(@Nonnull final String txId, final FedoraId parent, final FedoraId child) {
        final String parentID = parent.getFullId();
        final String childID = child.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", parentID);
        parameterSource.addValue("child", childID);
        parameterSource.addValue("transactionId", txId);
        parameterSource.addValue("endTime", formatInstant(Instant.now()));
        final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (addedInTxn) {
            jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION, parameterSource);
        } else {
            jdbcTemplate.update(DELETE_CHILD_IN_TRANSACTION, parameterSource);
        }
    }

    @Override
    public void removeResource(@Nonnull final String txId, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        parameterSource.addValue("transactionId", txId);
        final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT,
                parameterSource).isEmpty();
        if (addedInTxn) {
            jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION_NO_PARENT, parameterSource);
        } else {
            final String parent = getContainedBy(txId, resource);
            if (parent != null) {
                LOGGER.debug("Marking containment relationship between parent ({}) and child ({}) deleted", parent,
                        resourceID);
                parameterSource.addValue("parent", parent);
                parameterSource.addValue("endTime", formatInstant(Instant.now()));
                jdbcTemplate.update(DELETE_CHILD_IN_TRANSACTION, parameterSource);
            }
        }
    }

    @Override
    public void purgeResource(@Nonnull final String txId, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        parameterSource.addValue("transactionId", txId);
        final String parent = getContainedByDeleted(txId, resource);
        final boolean deletedInTxn = !jdbcTemplate.queryForList(IS_CHILD_DELETED_IN_TRANSACTION_NO_PARENT,
                parameterSource).isEmpty();
        if (deletedInTxn) {
            jdbcTemplate.update(UNDO_DELETE_CHILD_IN_TRANSACTION_NO_PARENT, parameterSource);
        }
        if (parent != null) {
            LOGGER.debug("Removing containment relationship between parent ({}) and child ({})", parent, resourceID);
            parameterSource.addValue("parent", parent);
            jdbcTemplate.update(PURGE_CHILD_IN_TRANSACTION, parameterSource);
        }
    }

    /**
     * Find parent for a resource using a deleted containment relationship.
     * @param txId the transaction id.
     * @param resource the child resource id.
     * @return the parent id.
     */
    private String getContainedByDeleted(final String txId, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final List<String> parentID;
        if (txId != null) {
            parameterSource.addValue("transactionId", txId);
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_DELETED_IN_TRANSACTION, parameterSource, String.class);
        } else {
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_DELETED, parameterSource, String.class);
        }
        return parentID.stream().findFirst().orElse(null);
    }

    @Transactional
    @Override
    public void commitTransaction(final String txId) {
        if (txId != null) {
            try {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue("transactionId", txId);
                final List<String> changedParents = jdbcTemplate.queryForList(GET_UPDATED_RESOURCES, parameterSource,
                        String.class);
                jdbcTemplate.update(COMMIT_PURGE_RECORDS, parameterSource);
                jdbcTemplate.update(COMMIT_DELETE_RECORDS.get(dbPlatform), parameterSource);
                jdbcTemplate.update(COMMIT_ADD_RECORDS, parameterSource);
                for (final var parent : changedParents) {
                    final var updated = jdbcTemplate.queryForObject(SELECT_LAST_UPDATED_IN_TX,
                            Map.of("resourceId", parent, "transactionId", txId), Instant.class);
                    if (updated != null) {
                        jdbcTemplate.update(UPDATE_LAST_UPDATED,
                                Map.of("resourceId", parent, "updated", formatInstant(updated)));
                    }
                }
                jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, parameterSource);
            } catch (final Exception e) {
                LOGGER.warn("Unable to commit containment index transaction {}: {}", txId, e.getMessage());
                throw new RepositoryRuntimeException("Unable to commit containment index transaction", e);
            }
        }
    }

    @Override
    public void rollbackTransaction(final String txId) {
        if (txId != null) {
            final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("transactionId", txId);
            jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, parameterSource);
        }
    }

    @Override
    public boolean resourceExists(final String txId, final FedoraId fedoraId, final boolean includeDeleted) {
        // Get the containing ID because fcr:metadata will not exist here but MUST exist if the containing resource does
        final String resourceId = fedoraId.getBaseId();
        LOGGER.debug("Checking if {} exists in transaction {}", resourceId, txId);
        if (fedoraId.isRepositoryRoot()) {
            // Root always exists.
            return true;
        }
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceId);
        final String queryToUse;
        if (txId != null) {
            queryToUse = includeDeleted ? RESOURCE_OR_TOMBSTONE_EXISTS_IN_TRANSACTION :
                    RESOURCE_EXISTS_IN_TRANSACTION;
            parameterSource.addValue("transactionId", txId);
        } else {
            queryToUse = includeDeleted ? RESOURCE_OR_TOMBSTONE_EXISTS :
                    RESOURCE_EXISTS;
        }
        return !jdbcTemplate.queryForList(queryToUse, parameterSource, String.class).isEmpty();
    }

    @Override
    public FedoraId getContainerIdByPath(final String txId, final FedoraId fedoraId, final boolean checkDeleted) {
        if (fedoraId.isRepositoryRoot()) {
            // If we are root then we are the top.
            return fedoraId;
        }
        final String parent = getContainedBy(txId, fedoraId);
        if (parent != null) {
            return FedoraId.create(parent);
        }
        String fullId = fedoraId.getFullId();
        while (fullId.contains("/")) {
            fullId = fedoraId.getResourceId().substring(0, fullId.lastIndexOf("/"));
            if (fullId.equals(FEDORA_ID_PREFIX)) {
                return FedoraId.getRepositoryRootId();
            }
            final FedoraId testID = FedoraId.create(fullId);
            if (resourceExists(txId, testID, checkDeleted)) {
                return testID;
            }
        }
        return FedoraId.getRepositoryRootId();
    }

    @Transactional
    @Override
    public void reset() {
        try {
            jdbcTemplate.update(TRUNCATE_TABLE + RESOURCES_TABLE, Collections.emptyMap());
            jdbcTemplate.update(TRUNCATE_TABLE + TRANSACTION_OPERATIONS_TABLE, Collections.emptyMap());
        } catch (final Exception e) {
            throw new RepositoryRuntimeException("Failed to truncate containment tables", e);
        }
    }

    @Override
    public boolean hasResourcesStartingWith(final String txId, final FedoraId fedoraId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("resourceId", fedoraId.getFullId() + "/%");
        final boolean matchingIds;
        if (txId != null) {
            parameterSource.addValue("transactionId", txId);
            matchingIds = !jdbcTemplate.queryForList(SELECT_ID_LIKE_IN_TRANSACTION, parameterSource, String.class)
                .isEmpty();
        } else {
            matchingIds = !jdbcTemplate.queryForList(SELECT_ID_LIKE, parameterSource, String.class).isEmpty();
        }
        return matchingIds;
    }

    @Override
    public Instant containmentLastUpdated(final String txId, final FedoraId fedoraId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("resourceId", fedoraId.getFullId());
        final String queryToUse;
        if (txId == null) {
            queryToUse = SELECT_LAST_UPDATED;
        } else {
            parameterSource.addValue("transactionId", txId);
            queryToUse = SELECT_LAST_UPDATED_IN_TX;
        }
        try {
            return jdbcTemplate.queryForObject(queryToUse, parameterSource, Instant.class);
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Get the data source backing this containment index
     * @return data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set the data source backing this containment index
     * @param dataSource data source
     */
    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Format an instant to a timestamp without milliseconds, due to precision
     * issues with memento datetimes.
     * @param instant the instant to format.
     * @return the datetime timestamp
     */
    private Timestamp formatInstant(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.from(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Private class to back a stream with a paged DB query.
     *
     * If this needs to be run in parallel we will have to override trySplit() and determine a good method to split on.
     */
    private class ContainmentIterator extends Spliterators.AbstractSpliterator<String> {
        final Queue<String> children = new ConcurrentLinkedQueue<>();
        int numOffsets = 0;
        final String queryToUse;
        final MapSqlParameterSource parameterSource;

        public ContainmentIterator(final String query, final MapSqlParameterSource parameters) {
            super(Long.MAX_VALUE, Spliterator.ORDERED);
            queryToUse = query;
            parameterSource = parameters;
            parameterSource.addValue("containsLimit", containsLimit);
        }

        @Override
        public boolean tryAdvance(final Consumer<? super String> action) {
            try {
                action.accept(children.remove());
            } catch (final NoSuchElementException e) {
                parameterSource.addValue("offSet", numOffsets * containsLimit);
                numOffsets += 1;
                children.addAll(jdbcTemplate.queryForList(queryToUse, parameterSource, String.class));
                if (children.size() == 0) {
                    // no more elements.
                    return false;
                }
                action.accept(children.remove());
            }
            return true;
        }
    }
}
