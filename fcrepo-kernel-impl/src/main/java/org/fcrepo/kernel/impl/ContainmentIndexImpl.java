/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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
     * Upsert a parent child relationship to the transaction operation table.
     */
    private static final String UPSERT_RECORDS_POSTGRESQL = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ") VALUES (:parent, :child, :startTime, :endTime, " +
            ":transactionId, :operation) ON CONFLICT ( " +  FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ") " +
            "DO UPDATE SET " + PARENT_COLUMN + " = EXCLUDED." + PARENT_COLUMN + ", " +
            START_TIME_COLUMN + " = EXCLUDED." + START_TIME_COLUMN + ", " + END_TIME_COLUMN + " = EXCLUDED." +
            END_TIME_COLUMN + ", " + OPERATION_COLUMN + " = EXCLUDED." + OPERATION_COLUMN;

    private static final String UPSERT_RECORDS_MYSQL_MARIA = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " (" + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ") VALUES (:parent, :child, :startTime, :endTime, " +
            ":transactionId, :operation) ON DUPLICATE KEY UPDATE " +
            PARENT_COLUMN + " = VALUES(" + PARENT_COLUMN + "), " + START_TIME_COLUMN + " = VALUES(" +
            START_TIME_COLUMN + "), " + END_TIME_COLUMN + " = VALUES(" + END_TIME_COLUMN + "), " + OPERATION_COLUMN +
            " = VALUES(" + OPERATION_COLUMN + ")";

    private static final String UPSERT_RECORDS_H2 = "MERGE INTO " + TRANSACTION_OPERATIONS_TABLE +
            " (" + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ") KEY (" + FEDORA_ID_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ") VALUES (:parent, :child, :startTime, :endTime, :transactionId, :operation)";

    private static final String DIRECT_UPDATE_END_TIME = "UPDATE " + RESOURCES_TABLE +
            " SET " + END_TIME_COLUMN + " = :endTime WHERE " +
            PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = :child";

    private static final Map<DbPlatform, String> UPSERT_MAPPING = Map.of(
            DbPlatform.H2, UPSERT_RECORDS_H2,
            DbPlatform.MYSQL, UPSERT_RECORDS_MYSQL_MARIA,
            DbPlatform.MARIADB, UPSERT_RECORDS_MYSQL_MARIA,
            DbPlatform.POSTGRESQL, UPSERT_RECORDS_POSTGRESQL
    );

    private static final String DIRECT_INSERT_RECORDS = "INSERT INTO " + RESOURCES_TABLE +
            " (" + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ")" +
            " VALUES (:parent, :child, :startTime, :endTime)";

    private static final String DIRECT_INSERT_POSTGRESQL = " ON CONFLICT ( " + FEDORA_ID_COLUMN + ")" +
            "DO UPDATE SET " + PARENT_COLUMN + " = EXCLUDED." + PARENT_COLUMN + ", " +
            START_TIME_COLUMN + " = EXCLUDED." + START_TIME_COLUMN + ", " +
            END_TIME_COLUMN + " = EXCLUDED." + END_TIME_COLUMN;

    private static final String DIRECT_INSERT_MYSQL_MARIA = " ON DUPLICATE KEY UPDATE " +
            PARENT_COLUMN + " = VALUES(" + PARENT_COLUMN + "), " + START_TIME_COLUMN + " = VALUES(" +
            START_TIME_COLUMN + "), " + END_TIME_COLUMN + " = VALUES(" + END_TIME_COLUMN + ")";

    private static final String DIRECT_INSERT_H2 = "MERGE INTO " + RESOURCES_TABLE +
            " (" + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ")" +
            " KEY (" + FEDORA_ID_COLUMN + ") VALUES (:parent, :child, :startTime, :endTime)";

    private static final Map<DbPlatform, String> DIRECT_UPSERT_MAPPING = Map.of(
        DbPlatform.H2, DIRECT_INSERT_H2,
        DbPlatform.MYSQL, DIRECT_INSERT_RECORDS + DIRECT_INSERT_MYSQL_MARIA,
        DbPlatform.MARIADB, DIRECT_INSERT_RECORDS + DIRECT_INSERT_MYSQL_MARIA,
        DbPlatform.POSTGRESQL, DIRECT_INSERT_RECORDS + DIRECT_INSERT_POSTGRESQL
    );

    private static final String DIRECT_PURGE = "DELETE FROM containment WHERE fedora_id = :child";

    /*
     * Remove an insert row from the transaction operation table for this parent child relationship.
     */
    private static final String UNDO_INSERT_CHILD_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    /*
     * Remove a mark as deleted row from the transaction operation table for this child relationship (no parent).
     */
    private static final String UNDO_DELETE_CHILD_IN_TRANSACTION_NO_PARENT = "DELETE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

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
    * Delete all rows from the transaction operation table for this transaction.
    */
    private static final String DELETE_ENTIRE_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId";

    /*
     * Add to the main table all rows from the transaction operation table marked 'add' for this transaction.
     */
    private static final String COMMIT_ADD_RECORDS_POSTGRESQL = "INSERT INTO " + RESOURCES_TABLE +
            " ( " + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ") " +
            "SELECT " + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN +
            " FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + OPERATION_COLUMN + " = 'add' AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId ON CONFLICT ( " +  FEDORA_ID_COLUMN + " )" +
            " DO UPDATE SET " + PARENT_COLUMN + " = EXCLUDED." + PARENT_COLUMN + ", " +
            START_TIME_COLUMN + " = EXCLUDED." + START_TIME_COLUMN + ", " + END_TIME_COLUMN + " = EXCLUDED." +
            END_TIME_COLUMN;

    private static final String COMMIT_ADD_RECORDS_MYSQL_MARIA = "INSERT INTO " + RESOURCES_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ") " +
            "SELECT " + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN +
            " FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + OPERATION_COLUMN + " = 'add' AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId ON DUPLICATE KEY UPDATE " +
            PARENT_COLUMN + " = VALUES(" + PARENT_COLUMN + "), " + START_TIME_COLUMN + " = VALUES(" +
            START_TIME_COLUMN + "), " + END_TIME_COLUMN + " = VALUES(" + END_TIME_COLUMN + ")";

    private static final String COMMIT_ADD_RECORDS_H2 = "MERGE INTO " + RESOURCES_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ", " + START_TIME_COLUMN + ", " + END_TIME_COLUMN + ") " +
            "KEY (" + FEDORA_ID_COLUMN + ") SELECT " + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ", " +
            START_TIME_COLUMN + ", " + END_TIME_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            OPERATION_COLUMN + " = 'add' AND " + TRANSACTION_ID_COLUMN + " = :transactionId";

    private static final Map<DbPlatform, String> COMMIT_ADD_RECORDS_MAP = Map.of(
            DbPlatform.H2, COMMIT_ADD_RECORDS_H2,
            DbPlatform.MYSQL, COMMIT_ADD_RECORDS_MYSQL_MARIA,
            DbPlatform.MARIADB, COMMIT_ADD_RECORDS_MYSQL_MARIA,
            DbPlatform.POSTGRESQL, COMMIT_ADD_RECORDS_POSTGRESQL
    );

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
            " WHERE EXISTS (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE + " t WHERE t." + FEDORA_ID_COLUMN +
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

    private final Map<DbPlatform, String> COMMIT_DELETE_RECORDS = Map.of(
            DbPlatform.H2, COMMIT_DELETE_RECORDS_H2,
            DbPlatform.MARIADB, COMMIT_DELETE_RECORDS_MYSQL,
            DbPlatform.MYSQL, COMMIT_DELETE_RECORDS_MYSQL,
            DbPlatform.POSTGRESQL, COMMIT_DELETE_RECORDS_POSTGRES
    );

    /*
     * Remove from the main table all rows from transaction operation table marked 'purge' for this transaction.
     */
    private static final String COMMIT_PURGE_RECORDS = "DELETE FROM " + RESOURCES_TABLE +
            " WHERE (" + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ") IN (" +
            " SELECT t." + FEDORA_ID_COLUMN + ", t." + PARENT_COLUMN +
            " FROM " + TRANSACTION_OPERATIONS_TABLE + " t " +
            " WHERE t." + TRANSACTION_ID_COLUMN + " = :transactionId " +
            " AND t." + OPERATION_COLUMN + " = 'purge')";
    private static final String COMMIT_PURGE_RECORDS_POSTGRES = "DELETE FROM " + RESOURCES_TABLE + " r" +
            " USING " + TRANSACTION_OPERATIONS_TABLE + " t" +
            " WHERE t." + FEDORA_ID_COLUMN + " = r." + FEDORA_ID_COLUMN +
            " AND t." + PARENT_COLUMN + " = r." + PARENT_COLUMN +
            " AND t." + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND t." + OPERATION_COLUMN + " = 'purge'";
    private static final String COMMIT_PURGE_RECORDS_MYSQL = "DELETE r" +
            " FROM " + RESOURCES_TABLE + " r" +
            " INNER JOIN " + TRANSACTION_OPERATIONS_TABLE + " t" +
            " ON t." + FEDORA_ID_COLUMN + " = r." + FEDORA_ID_COLUMN +
            " AND t." + PARENT_COLUMN + " = r." + PARENT_COLUMN +
            " WHERE t." + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND t." + OPERATION_COLUMN + " = 'purge'";

    private static final Map<DbPlatform, String> COMMIT_PURGE_RECORDS_MAP = Map.of(
            DbPlatform.H2, COMMIT_PURGE_RECORDS,
            DbPlatform.MYSQL, COMMIT_PURGE_RECORDS_MYSQL,
            DbPlatform.MARIADB, COMMIT_PURGE_RECORDS_MYSQL,
            DbPlatform.POSTGRESQL, COMMIT_PURGE_RECORDS_POSTGRES
    );
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

    private static final String SELECT_LAST_UPDATED = "SELECT " + UPDATED_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :resourceId";

    private static final String UPDATE_LAST_UPDATED = "UPDATE " + RESOURCES_TABLE + " SET " + UPDATED_COLUMN +
            " = :updated WHERE " + FEDORA_ID_COLUMN + " = :resourceId";

    private static final String CONDITIONALLY_UPDATE_LAST_UPDATED = "UPDATE " + RESOURCES_TABLE +
            " SET " + UPDATED_COLUMN + " = :updated WHERE " + FEDORA_ID_COLUMN + " = :resourceId" +
            " AND (" + UPDATED_COLUMN + " IS NULL OR " + UPDATED_COLUMN + " < :updated)";

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

    /*
     * Get the startTime for the specified resource from the main table, if it exists.
     */
    private static final String GET_START_TIME = "SELECT " + START_TIME_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child";

    /*
     * Get all resources deleted in this transaction
     */
    private static final String GET_DELETED_RESOURCES = "SELECT " + FEDORA_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'delete'";

    /*
     * Get all resources added in this transaction
     */
    private static final String GET_ADDED_RESOURCES = "SELECT " + FEDORA_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    @Inject
    private FedoraPropsConfig fedoraPropsConfig;

    private Cache<String, String> getContainedByCache;

    private Cache<String, Boolean> resourceExistsCache;

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() {
        jdbcTemplate = getNamedParameterJdbcTemplate();
        dbPlatform = DbPlatform.fromDataSource(dataSource);
        this.getContainedByCache = Caffeine.newBuilder()
                .maximumSize(fedoraPropsConfig.getContainmentCacheSize())
                .expireAfterAccess(fedoraPropsConfig.getContainmentCacheTimeout(), TimeUnit.MINUTES)
                .build();
        this.resourceExistsCache = Caffeine.newBuilder()
                .maximumSize(fedoraPropsConfig.getContainmentCacheSize())
                .expireAfterAccess(fedoraPropsConfig.getContainmentCacheTimeout(), TimeUnit.MINUTES)
                .build();
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(getDataSource());
    }

    void setContainsLimit(final int limit) {
        containsLimit = limit;
    }

    @Override
    public Stream<String> getContains(@Nonnull final Transaction tx, final FedoraId fedoraId) {
        final String resourceId = fedoraId.isMemento() ? fedoraId.getBaseId() : fedoraId.getFullId();
        final Instant asOfTime = fedoraId.isMemento() ? fedoraId.getMementoInstant() : null;
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", resourceId);

        LOGGER.debug("getContains for {} in transaction {} and instant {}", resourceId, tx, asOfTime);

        final String query;
        if (asOfTime == null) {
            if (tx.isOpenLongRunning()) {
                // we are in a transaction
                parameterSource.addValue("transactionId", tx.getId());
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
    public Stream<String> getContainsDeleted(@Nonnull final Transaction tx, final FedoraId fedoraId) {
        final String resourceId = fedoraId.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", resourceId);

        final String query;
        if (tx.isOpenLongRunning()) {
            // we are in a transaction
            parameterSource.addValue("transactionId", tx.getId());
            query = SELECT_DELETED_CHILDREN_IN_TRANSACTION;
        } else {
            // not in a transaction
            query = SELECT_DELETED_CHILDREN;
        }
        LOGGER.debug("getContainsDeleted for {} in transaction {}", resourceId, tx);
        return StreamSupport.stream(new ContainmentIterator(query, parameterSource), false);
    }

    @Override
    public String getContainedBy(@Nonnull final Transaction tx, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final String parentID;
        if (tx.isOpenLongRunning()) {
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_IN_TRANSACTION, Map.of("child", resourceID,
                    "transactionId", tx.getId()), String.class).stream().findFirst().orElse(null);
        } else {
            parentID = this.getContainedByCache.get(resourceID, key ->
                    jdbcTemplate.queryForList(PARENT_EXISTS, Map.of("child", key), String.class).stream()
                    .findFirst().orElse(null)
            );
        }
        return parentID;
    }

    @Override
    public void addContainedBy(@Nonnull final Transaction tx, final FedoraId parent, final FedoraId child) {
        addContainedBy(tx, parent, child, Instant.now(), null);
    }

    @Override
    public void addContainedBy(@Nonnull final Transaction tx, final FedoraId parent, final FedoraId child,
                               final Instant startTime, final Instant endTime) {
        // Don't add containment for these types of children
        if (childShouldNotBeContained(child)) {
            return;
        }
        tx.doInTx(() -> {
            final String parentID = parent.getFullId();
            final String childID = child.getFullId();

            if (!tx.isShortLived()) {
                LOGGER.debug("Adding: parent: {}, child: {}, in txn: {}, start time {}, end time {}", parentID, childID,
                        tx.getId(), formatInstant(startTime), formatInstant(endTime));
                doUpsert(tx, parentID, childID, startTime, endTime, "add");
            } else {
                LOGGER.debug("Adding: parent: {}, child: {}, start time {}, end time {}", parentID, childID,
                        formatInstant(startTime), formatInstant(endTime));
                doDirectUpsert(parentID, childID, startTime, endTime);
            }
        });
    }

    private boolean childShouldNotBeContained(final FedoraId child) {
        return child.isAcl();
    }

    @Override
    public void removeContainedBy(@Nonnull final Transaction tx, final FedoraId parent, final FedoraId child) {
        tx.doInTx(() -> {
            final String parentID = parent.getFullId();
            final String childID = child.getFullId();

            if (!tx.isShortLived()) {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue("parent", parentID);
                parameterSource.addValue("child", childID);
                parameterSource.addValue("transactionId", tx.getId());
                final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION, parameterSource)
                        .isEmpty();
                if (addedInTxn) {
                    jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION, parameterSource);
                } else {
                    doUpsert(tx, parentID, childID, null, Instant.now(), "delete");
                }
            } else {
                doDirectUpsert(parentID, childID, null, Instant.now());
                this.getContainedByCache.invalidate(childID);
            }
        });
    }

    @Override
    public void removeResource(@Nonnull final Transaction tx, final FedoraId resource) {
        tx.doInTx(() -> {
            final String resourceID = resource.getFullId();

            if (!tx.isShortLived()) {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue("child", resourceID);
                parameterSource.addValue("transactionId", tx.getId());
                final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT,
                        parameterSource).isEmpty();
                if (addedInTxn) {
                    jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION_NO_PARENT, parameterSource);
                } else {
                    final String parent = getContainedBy(tx, resource);
                    if (parent != null) {
                        LOGGER.debug("Marking containment relationship between parent ({}) and child ({}) deleted",
                                parent, resourceID);
                        doUpsert(tx, parent, resourceID, null, Instant.now(), "delete");
                    }
                }
            } else {
                final String parent = getContainedBy(tx, resource);
                if (parent != null) {
                    LOGGER.debug("Marking containment relationship between parent ({}) and child ({}) deleted", parent,
                            resourceID);
                    doDirectUpsert(parent, resourceID, null, Instant.now());
                    this.getContainedByCache.invalidate(resourceID);
                }
            }
        });
    }

    @Override
    public void purgeResource(@Nonnull final Transaction tx, final FedoraId resource) {
        tx.doInTx(() -> {
            final String resourceID = resource.getFullId();

            final String parent = getContainedByDeleted(tx, resource);

            if (parent != null) {
                LOGGER.debug("Removing containment relationship between parent ({}) and child ({})",
                        parent, resourceID);

                if (!tx.isShortLived()) {
                    doUpsert(tx, parent, resourceID, null, null, "purge");
                } else {
                    final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                    parameterSource.addValue("child", resourceID);
                    jdbcTemplate.update(DIRECT_PURGE, parameterSource);
                }
            }
        });
    }

    /**
     * Do the Upsert action to the transaction table.
     * @param tx the transaction
     * @param parentId the containing resource id
     * @param resourceId the contained resource id
     * @param startTime the instant the relationship started, if null get the current time from the main table.
     * @param endTime the instant the relationship ended or null for none.
     * @param operation the operation to perform.
     */
    private void doUpsert(final Transaction tx, final String parentId, final String resourceId, final Instant startTime,
                          final Instant endTime, final String operation) {
        final var parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceId);
        parameterSource.addValue("transactionId", tx.getId());
        parameterSource.addValue("parent", parentId);
        if (startTime == null) {
            parameterSource.addValue("startTime", formatInstant(getCurrentStartTime(resourceId)));
        } else {
            parameterSource.addValue("startTime", formatInstant(startTime));
        }
        parameterSource.addValue("endTime", formatInstant(endTime));
        parameterSource.addValue("operation", operation);
        jdbcTemplate.update(UPSERT_MAPPING.get(dbPlatform), parameterSource);
    }

    /**
     * Do the Upsert directly to the containment index; not the tx table
     *
     * @param parentId the containing resource id
     * @param resourceId the contained resource id
     * @param startTime the instant the relationship started, if null get the current time from the main table.
     * @param endTime the instant the relationship ended or null for none.
     */
    private void doDirectUpsert(final String parentId, final String resourceId, final Instant startTime,
                                final Instant endTime) {
        final var parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceId);
        parameterSource.addValue("parent", parentId);
        parameterSource.addValue("endTime", formatInstant(endTime));

        final String query;

        if (startTime == null) {
            // This the case for an update
            query = DIRECT_UPDATE_END_TIME;
        } else {
            // This is the case for a new record
            parameterSource.addValue("startTime", formatInstant(startTime));
            query = DIRECT_UPSERT_MAPPING.get(dbPlatform);
        }

        jdbcTemplate.update(query, parameterSource);
        updateParentTimestamp(parentId, startTime, endTime);
        resourceExistsCache.invalidate(resourceId);
    }

    private void updateParentTimestamp(final String parentId, final Instant startTime, final Instant endTime) {
        final var parameterSource = new MapSqlParameterSource();
        final var updated = endTime == null ? startTime : endTime;
        parameterSource.addValue("resourceId", parentId);
        parameterSource.addValue("updated", formatInstant(updated));
        jdbcTemplate.update(CONDITIONALLY_UPDATE_LAST_UPDATED, parameterSource);
    }

    /**
     * Find parent for a resource using a deleted containment relationship.
     * @param tx the transaction.
     * @param resource the child resource id.
     * @return the parent id.
     */
    private String getContainedByDeleted(final Transaction tx, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final List<String> parentID;
        if (tx.isOpenLongRunning()) {
            parameterSource.addValue("transactionId", tx.getId());
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_DELETED_IN_TRANSACTION, parameterSource, String.class);
        } else {
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_DELETED, parameterSource, String.class);
        }
        return parentID.stream().findFirst().orElse(null);
    }

    @Override
    public void commitTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            tx.ensureCommitting();
            try {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue("transactionId", tx.getId());
                final List<String> changedParents = jdbcTemplate.queryForList(GET_UPDATED_RESOURCES, parameterSource,
                        String.class);
                final List<String> removedResources = jdbcTemplate.queryForList(GET_DELETED_RESOURCES, parameterSource,
                        String.class);
                final List<String> addedResources = jdbcTemplate.queryForList(GET_ADDED_RESOURCES, parameterSource,
                        String.class);
                final int purged = jdbcTemplate.update(COMMIT_PURGE_RECORDS_MAP.get(dbPlatform), parameterSource);
                final int deleted = jdbcTemplate.update(COMMIT_DELETE_RECORDS.get(dbPlatform), parameterSource);
                final int added = jdbcTemplate.update(COMMIT_ADD_RECORDS_MAP.get(dbPlatform), parameterSource);
                for (final var parent : changedParents) {
                    final var updated = jdbcTemplate.queryForObject(SELECT_LAST_UPDATED_IN_TX,
                            Map.of("resourceId", parent, "transactionId", tx.getId()), Timestamp.class);
                    if (updated != null) {
                        jdbcTemplate.update(UPDATE_LAST_UPDATED,
                                Map.of("resourceId", parent, "updated", updated));
                    }
                }
                jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, parameterSource);
                this.getContainedByCache.invalidateAll(removedResources);
                // Add inserted records to removed records list.
                removedResources.addAll(addedResources);
                this.resourceExistsCache.invalidateAll(removedResources);
                LOGGER.debug("Commit of tx {} complete with {} adds, {} deletes and {} purges",
                        tx.getId(), added, deleted, purged);
            } catch (final Exception e) {
                LOGGER.warn("Unable to commit containment index transaction {}: {}", tx, e.getMessage());
                throw new RepositoryRuntimeException("Unable to commit containment index transaction", e);
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void rollbackTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("transactionId", tx.getId());
            jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, parameterSource);
        }
    }

    @Override
    public void clearAllTransactions() {
        jdbcTemplate.update(TRUNCATE_TABLE + TRANSACTION_OPERATIONS_TABLE, Collections.emptyMap());
    }

    @Override
    public boolean resourceExists(@Nonnull final Transaction tx, final FedoraId fedoraId,
                                  final boolean includeDeleted) {
        // Get the containing ID because fcr:metadata will not exist here but MUST exist if the containing resource does
        final String resourceId = fedoraId.getBaseId();
        LOGGER.debug("Checking if {} exists in transaction {}", resourceId, tx);
        if (fedoraId.isRepositoryRoot()) {
            // Root always exists.
            return true;
        }
        if (tx.isOpenLongRunning()) {
            final var queryToUse = includeDeleted ? RESOURCE_OR_TOMBSTONE_EXISTS_IN_TRANSACTION :
                    RESOURCE_EXISTS_IN_TRANSACTION;
            return !jdbcTemplate.queryForList(queryToUse,
                    Map.of("child", resourceId, "transactionId", tx.getId()), String.class).isEmpty();
        } else if (includeDeleted) {
            final Boolean exists = resourceExistsCache.getIfPresent(resourceId);
            if (exists != null && exists) {
                // Only return true, false values might change once deleted resources are included.
                return true;
            }
            return !jdbcTemplate.queryForList(RESOURCE_OR_TOMBSTONE_EXISTS,
                    Map.of("child", resourceId), String.class).isEmpty();
        } else {
            return resourceExistsCache.get(resourceId, key -> !jdbcTemplate.queryForList(RESOURCE_EXISTS,
                        Map.of("child", resourceId), String.class).isEmpty()
            );
        }
    }

    @Override
    public FedoraId getContainerIdByPath(final Transaction tx, final FedoraId fedoraId, final boolean checkDeleted) {
        if (fedoraId.isRepositoryRoot()) {
            // If we are root then we are the top.
            return fedoraId;
        }
        final String parent = getContainedBy(tx, fedoraId);
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
            if (resourceExists(tx, testID, checkDeleted)) {
                return testID;
            }
        }
        return FedoraId.getRepositoryRootId();
    }

    @Override
    public void reset() {
        try {
            jdbcTemplate.update(TRUNCATE_TABLE + RESOURCES_TABLE, Collections.emptyMap());
            jdbcTemplate.update(TRUNCATE_TABLE + TRANSACTION_OPERATIONS_TABLE, Collections.emptyMap());
            this.getContainedByCache.invalidateAll();
        } catch (final Exception e) {
            throw new RepositoryRuntimeException("Failed to truncate containment tables", e);
        }
    }

    @Override
    public boolean hasResourcesStartingWith(final Transaction tx, final FedoraId fedoraId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        String resourceId = fedoraId.getFullId();
        if (resourceId.contains("_")) {
            resourceId = resourceId.replaceAll("_", "\\\\_");
        }
        if (resourceId.contains("%")) {
            resourceId = resourceId.replaceAll("%", "\\\\%");
        }
        parameterSource.addValue("resourceId", resourceId + "/%");
        final boolean matchingIds;
        if (tx.isOpenLongRunning()) {
            parameterSource.addValue("transactionId", tx.getId());
            matchingIds = !jdbcTemplate.queryForList(SELECT_ID_LIKE_IN_TRANSACTION, parameterSource, String.class)
                .isEmpty();
        } else {
            matchingIds = !jdbcTemplate.queryForList(SELECT_ID_LIKE, parameterSource, String.class).isEmpty();
        }
        return matchingIds;
    }

    @Override
    public Instant containmentLastUpdated(final Transaction tx, final FedoraId fedoraId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("resourceId", fedoraId.getFullId());
        final String queryToUse;
        if (tx.isOpenLongRunning()) {
            parameterSource.addValue("transactionId", tx.getId());
            queryToUse = SELECT_LAST_UPDATED_IN_TX;
        } else {
            queryToUse = SELECT_LAST_UPDATED;
        }
        try {
            return fromTimestamp(jdbcTemplate.queryForObject(queryToUse, parameterSource, Timestamp.class));
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
     * Get the current startTime for the resource
     * @param resourceId id of the resource
     * @return start time or null if no committed record.
     */
    private Instant getCurrentStartTime(final String resourceId) {
        return fromTimestamp(jdbcTemplate.queryForObject(GET_START_TIME, Map.of(
                "child", resourceId
        ), Timestamp.class));
    }

    private Instant fromTimestamp(final Timestamp timestamp) {
        if (timestamp != null) {
            return timestamp.toInstant();
        }
        return null;
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
