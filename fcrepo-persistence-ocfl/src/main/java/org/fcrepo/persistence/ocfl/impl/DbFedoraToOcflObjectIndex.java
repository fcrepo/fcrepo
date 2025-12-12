/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.persistence.ocfl.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.cache.Cache;
import org.fcrepo.storage.ocfl.cache.CaffeineCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Maps Fedora IDs to the OCFL IDs of the OCFL objects the Fedora resource is stored in. This implementation is backed
 * by a relational database.
 *
 * @author pwinckles
 */
@Component("ocflIndexImpl")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DbFedoraToOcflObjectIndex implements FedoraToOcflObjectIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbFedoraToOcflObjectIndex.class);

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

    private static final String DIRECT_INSERT_MAPPING = "INSERT INTO " + MAPPING_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ")" +
            " VALUES (:fedoraId, :fedoraRootId, :ocflId)";

    private static final String DIRECT_INSERT_POSTGRESQL = " ON CONFLICT (" + FEDORA_ID_COLUMN + ")" +
            " DO UPDATE SET " + FEDORA_ROOT_ID_COLUMN + " = EXCLUDED." + FEDORA_ROOT_ID_COLUMN + ", " +
            OCFL_ID_COLUMN + " = EXCLUDED." + OCFL_ID_COLUMN;

    private static final String DIRECT_INSERT_MYSQL_MARIA = " ON DUPLICATE KEY UPDATE " +
            FEDORA_ROOT_ID_COLUMN + " = VALUES(" + FEDORA_ROOT_ID_COLUMN + "), " + OCFL_ID_COLUMN +
            " = VALUES(" + OCFL_ID_COLUMN + ")";

    private static final String DIRECT_INSERT_H2 = "MERGE INTO " + MAPPING_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ") " +
            " KEY (" + FEDORA_ID_COLUMN + ")" +
            " VALUES (:fedoraId, :fedoraRootId, :ocflId)";

    private static final Map<DbPlatform, String> DIRECT_INSERT_MAP = Map.of(
        DbPlatform.H2, DIRECT_INSERT_H2,
        DbPlatform.MYSQL, DIRECT_INSERT_MAPPING + DIRECT_INSERT_MYSQL_MARIA,
        DbPlatform.MARIADB, DIRECT_INSERT_MAPPING + DIRECT_INSERT_MYSQL_MARIA,
        DbPlatform.POSTGRESQL, DIRECT_INSERT_MAPPING + DIRECT_INSERT_POSTGRESQL
    );

    /**
     * Map of database product to UPSERT into operations table SQL.
     */
    private static final Map<DbPlatform, String> UPSERT_MAPPING_TX_MAP = Map.of(
            DbPlatform.MYSQL, UPSERT_MAPPING_TX_MYSQL_MARIA,
            DbPlatform.H2, UPSERT_MAPPING_TX_H2,
            DbPlatform.POSTGRESQL, UPSERT_MAPPING_TX_POSTGRESQL,
            DbPlatform.MARIADB, UPSERT_MAPPING_TX_MYSQL_MARIA
    );

    private static final String DIRECT_DELETE_MAPPING = "DELETE FROM ocfl_id_map WHERE fedora_id = :fedoraId";

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
    private static final String COMMIT_DELETE_RECORDS_H2 = "DELETE FROM " + MAPPING_TABLE + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " +  OPERATION_COLUMN + " = 'delete' AND " +
            MAPPING_TABLE + "." + FEDORA_ID_COLUMN + " = " + TRANSACTION_OPERATIONS_TABLE + "." + FEDORA_ID_COLUMN +
            ")";
    private static final String COMMIT_DELETE_RECORDS_MYSQL = "DELETE mt" +
            " FROM " + MAPPING_TABLE + " mt JOIN " + TRANSACTION_OPERATIONS_TABLE + " tot" +
            " ON mt." + FEDORA_ID_COLUMN + " = tot." + FEDORA_ID_COLUMN +
            " WHERE tot." + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND tot." + OPERATION_COLUMN + " = 'delete'";
    private static final String COMMIT_DELETE_RECORDS_POSTGRES = "DELETE FROM " + MAPPING_TABLE + " mt" +
            " USING " + TRANSACTION_OPERATIONS_TABLE + " tot" +
            " WHERE  tot." + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND mt." + FEDORA_ID_COLUMN + " = tot." + FEDORA_ID_COLUMN +
            " AND tot." + OPERATION_COLUMN + " = 'delete'";

    private static final Map<DbPlatform, String> COMMIT_DELETE_RECORDS_MAP = Map.of(
            DbPlatform.MYSQL, COMMIT_DELETE_RECORDS_MYSQL,
            DbPlatform.MARIADB, COMMIT_DELETE_RECORDS_MYSQL,
            DbPlatform.H2, COMMIT_DELETE_RECORDS_H2,
            DbPlatform.POSTGRESQL, COMMIT_DELETE_RECORDS_POSTGRES
    );

    /*
     * Collect IDs to invalidate on transaction commit.
     */
    private static final String GET_DELETE_IDS = "SELECT " + FEDORA_ID_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'delete'";

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

    private Cache<String, FedoraOcflMapping> mappingCache;

    private final DataSource dataSource;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private DbPlatform dbPlatform;

    @Inject
    private OcflPropsConfig ocflPropsConfig;

    public DbFedoraToOcflObjectIndex(@Autowired final DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @PostConstruct
    public void setup() {
        dbPlatform = DbPlatform.fromDataSource(dataSource);
        final var cache = Caffeine.newBuilder()
                .maximumSize(ocflPropsConfig.getFedoraToOcflCacheSize())
                .expireAfterAccess(ocflPropsConfig.getFedoraToOcflCacheTimeout(), TimeUnit.MINUTES)
                .build();
        this.mappingCache = new CaffeineCache<>(cache);
    }

    @Override
    public FedoraOcflMapping getMapping(final Transaction transaction, final FedoraId fedoraId)
            throws FedoraOcflMappingNotFoundException {
        try {
            if (transaction.isOpenLongRunning()) {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue("fedoraId", fedoraId.getResourceId());
                parameterSource.addValue("transactionId", transaction.getId());
                return jdbcTemplate.queryForObject(LOOKUP_MAPPING_IN_TRANSACTION, parameterSource,
                        GET_MAPPING_ROW_MAPPER);
            } else {
                return this.mappingCache.get(fedoraId.getResourceId(), key ->
                        jdbcTemplate.queryForObject(LOOKUP_MAPPING, Map.of("fedoraId", key), GET_MAPPING_ROW_MAPPER)
                );
            }
        } catch (final EmptyResultDataAccessException e) {
            throw new FedoraOcflMappingNotFoundException("No OCFL mapping found for " + fedoraId);
        }
    }

    @Override
    public FedoraOcflMapping addMapping(@Nonnull final Transaction transaction, final FedoraId fedoraId,
                                        final FedoraId fedoraRootId, final String ocflId) {
        transaction.doInTx(() -> {
            if (!transaction.isShortLived()) {
                upsert(transaction, fedoraId, "add", fedoraRootId, ocflId);
            } else {
                directInsert(fedoraId, fedoraRootId, ocflId);
            }
        });

        return new FedoraOcflMapping(fedoraRootId, ocflId);
    }

    @Override
    public void removeMapping(@Nonnull final Transaction transaction, final FedoraId fedoraId) {
        transaction.doInTx(() -> {
            if (!transaction.isShortLived()) {
                upsert(transaction, fedoraId, "delete");
            } else {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue("fedoraId", fedoraId.getResourceId());
                jdbcTemplate.update(DIRECT_DELETE_MAPPING, parameterSource);
                this.mappingCache.invalidate(fedoraId.getResourceId());
            }
        });
    }

    private void upsert(final Transaction transaction, final FedoraId fedoraId, final String operation) {
        upsert(transaction, fedoraId, operation, null, null);
    }

    /**
     * Perform the upsert to the operations table.
     *
     * @param transaction the transaction/session id.
     * @param fedoraId the resource id.
     * @param operation the operation we are performing (add or delete)
     * @param fedoraRootId the fedora root id (for add only)
     * @param ocflId the ocfl id (for add only).
     */
    private void upsert(final Transaction transaction, final FedoraId fedoraId, final String operation,
                        final FedoraId fedoraRootId, final String ocflId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("fedoraId", fedoraId.getResourceId());
        parameterSource.addValue("fedoraRootId", fedoraRootId == null ? null : fedoraRootId.getResourceId());
        parameterSource.addValue("ocflId", ocflId);
        parameterSource.addValue("transactionId", transaction.getId());
        parameterSource.addValue("operation", operation);
        try {
            jdbcTemplate.update(UPSERT_MAPPING_TX_MAP.get(dbPlatform), parameterSource);
        } catch (final DataIntegrityViolationException | BadSqlGrammarException e) {
            handleInsertException(fedoraId, e);
        }
    }

    private void directInsert(final FedoraId fedoraId, final FedoraId fedoraRootId, final String ocflId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("fedoraId", fedoraId.getResourceId());
        parameterSource.addValue("fedoraRootId", fedoraRootId == null ? null : fedoraRootId.getResourceId());
        parameterSource.addValue("ocflId", ocflId);
        try {
            jdbcTemplate.update(DIRECT_INSERT_MAP.get(dbPlatform), parameterSource);
        } catch (final DataIntegrityViolationException | BadSqlGrammarException e) {
            handleInsertException(fedoraId, e);
        }
    }

    @Override
    public void reset() {
        try {
            jdbcTemplate.update(TRUNCATE_MAPPINGS, Collections.emptyMap());
            jdbcTemplate.update(TRUNCATE_TRANSACTIONS, Collections.emptyMap());
            this.mappingCache.invalidateAll();
        } catch (final Exception e) {
            throw new RepositoryRuntimeException("Failed to truncate FedoraToOcfl index tables", e);
        }
    }

    @Override
    public void commit(@Nonnull final Transaction transaction) {
        if (!transaction.isShortLived()) {
            transaction.ensureCommitting();

            LOGGER.debug("Committing FedoraToOcfl index changes from transaction {}", transaction.getId());
            final Map<String, String> map = Map.of("transactionId", transaction.getId());
            try {
                final List<String> deleteIds = jdbcTemplate.queryForList(GET_DELETE_IDS, map, String.class);
                jdbcTemplate.update(COMMIT_DELETE_RECORDS_MAP.get(dbPlatform), map);
                jdbcTemplate.update(COMMIT_ADD_MAPPING_MAP.get(dbPlatform), map);
                jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, map);
                this.mappingCache.invalidateAll(deleteIds);
            } catch (final Exception e) {
                LOGGER.warn("Unable to commit FedoraToOcfl index transaction {}: {}", transaction, e.getMessage());
                throw new RepositoryRuntimeException("Unable to commit FedoraToOcfl index transaction", e);
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void rollback(@Nonnull final Transaction transaction) {
        if (!transaction.isShortLived()) {
            jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, Map.of("transactionId", transaction.getId()));
        }
    }

    private void handleInsertException(final FedoraId fedoraId, final Exception e) {
        if (e.getMessage().contains("too long for") || e.getCause().getMessage().contains("too long for")) {
            throw new InvalidResourceIdentifierException("Database error - Fedora ID path too long",e);
        } else if (e instanceof DuplicateKeyException) {
            throw new RepositoryRuntimeException("Database error - primary key already exists for Fedora ID: " +
                                                 fedoraId, e);
        } else {
            throw new RepositoryRuntimeException("Database error - error during upsert",e);
        }
    }

    @Override
    public void clearAllTransactions() {
        try {
            jdbcTemplate.update(TRUNCATE_TRANSACTIONS, Collections.emptyMap());
        } catch (final Exception e) {
            throw new RepositoryRuntimeException("Failed to truncate FedoraToOcfl transactions index tables", e);
        }
    }
}
