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

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.slf4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author peichman
 */
@Component
public class ContainmentIndexImpl implements ContainmentIndex {

    private static final Logger LOGGER = getLogger(ContainmentIndexImpl.class);

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Inject
    private PlatformTransactionManager platformTransactionManager;

    private static final String RESOURCES_TABLE = "resources";

    private static final String TRANSACTION_OPERATIONS_TABLE = "transaction_operations";

    private static final String FEDORA_ID_COLUMN = "fedora_id";

    private static final String PARENT_COLUMN = "parent";

    private static final String TRANSACTION_ID_COLUMN = "transaction_id";

    private static final String OPERATION_COLUMN = "operation";

    private static final String IS_DELETED_COLUMN = "is_deleted";

    /*
     * Select children of a resource that are not marked as deleted.
     */
    private static final String SELECT_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent AND " + IS_DELETED_COLUMN + " = FALSE";

    /*
     * Select children of a parent from resources table and from the transaction table with an 'add' operation,
     * but exclude any records that also exist in the transaction table with a 'delete' or 'purge' operation.
     */
    private static final String SELECT_CHILDREN_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent" +
            " AND " + IS_DELETED_COLUMN + " = FALSE" +
            " UNION SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = x." + FEDORA_ID_COLUMN +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " IN ('delete', 'purge'))";

    /*
     * Select all children of a resource that are marked for deletion.
     */
    private static final String SELECT_DELETED_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent AND " + IS_DELETED_COLUMN + " = TRUE";

    /*
     * Select children of a resource plus children 'delete'd in the non-committed transaction, but excluding any
     * 'add'ed in the non-committed transaction.
     */
    private static final String SELECT_DELETED_CHILDREN_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN +
            " FROM (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + IS_DELETED_COLUMN + " = TRUE UNION" +
            " SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            PARENT_COLUMN + " = :parent AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'delete') x" +
            " WHERE NOT EXISTS " +
            "(SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + PARENT_COLUMN + " = :parent AND " +
            FEDORA_ID_COLUMN + " = x." + FEDORA_ID_COLUMN + " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add')";

    /*
     * Insert a parent child relationship to the transaction operation table.
     */
    private static final String INSERT_CHILD_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN +
            " ) VALUES (:parent, :child, :transactionId, 'add')";

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
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN +
            " ) VALUES (:parent, :child, :transactionId, 'delete')";

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
            + PARENT_COLUMN + " ) SELECT " + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    /*
     * Mark deleted in the main table all rows from transaction operation table marked 'delete' for this transaction.
     */
    private static final String COMMIT_DELETE_RECORDS = "UPDATE " + RESOURCES_TABLE + " r " +
            "SET r." + IS_DELETED_COLUMN + " = TRUE WHERE EXISTS " +
            "(SELECT * from " + TRANSACTION_OPERATIONS_TABLE + " t WHERE " +
            "r." + FEDORA_ID_COLUMN + " = " + "t." + FEDORA_ID_COLUMN + " AND " +
            "t." + TRANSACTION_ID_COLUMN + " = :transactionId AND t." +  OPERATION_COLUMN + " = 'delete' AND " +
            "r." + PARENT_COLUMN + " = t." + PARENT_COLUMN + ")";

    /*
     * Remove from the main table all rows from transaction operation table marked 'purge' for this transaction.
     */
    private static final String COMMIT_PURGE_RECORDS = "DELETE FROM " + RESOURCES_TABLE + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " +  OPERATION_COLUMN + " = 'purge' AND " +
            RESOURCES_TABLE + "." + FEDORA_ID_COLUMN + " = " + TRANSACTION_OPERATIONS_TABLE + "." + FEDORA_ID_COLUMN +
            " AND " + RESOURCES_TABLE + "." + PARENT_COLUMN + " = " + TRANSACTION_OPERATIONS_TABLE + "." +
            PARENT_COLUMN + ")";

    /*
     * Query if a resource exists in the main table and is not deleted.
     */
    private static final String RESOURCE_EXISTS = "SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + IS_DELETED_COLUMN + " = FALSE";

    /*
     * Resource exists as a record in the transaction operations table with an 'add' operation and not also
     * exists as a 'delete' operation.
     */
    private static final String RESOURCE_EXISTS_IN_TRANSACTION = "SELECT " + FEDORA_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            " UNION SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'delete')";

    /*
     * Get the parent ID for this resource from the main table if not deleted.
     */
    private static final String PARENT_EXISTS = "SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + IS_DELETED_COLUMN + " = FALSE";

    /*
     * Get the parent ID for this resource from the operations table for an 'add' operation in this transaction, but
     * exclude any 'delete' operations for this resource in this transaction.
     */
    private static final String PARENT_EXISTS_IN_TRANSACTION= "SELECT x." + PARENT_COLUMN + " FROM" +
            " (SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
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
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + IS_DELETED_COLUMN + " = TRUE";

    /*
     * Get the parent ID for this resource from main table and the operations table for a 'delete' operation in this
     * transaction, excluding any 'add' operations for this resource in this transaction.
     */
    private static final String PARENT_EXISTS_DELETED_IN_TRANSACTION = "SELECT x." + PARENT_COLUMN + " FROM" +
            " (SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            " UNION SELECT " + PARENT_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN +
            " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete') x" +
            " WHERE NOT EXISTS " +
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

    private static final Map<String, String> DDL_MAP = Map.of(
            "MySQL", "sql/mysql-containment.sql",
            "H2", "sql/default-containment.sql",
            "PostgreSQL", "sql/default-containment.sql",
            "MariaDB", "sql/default-containment.sql"
    );

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() {
        jdbcTemplate = getNamedParameterJdbcTemplate();

        final var ddl = lookupDdl();
        LOGGER.info("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                dataSource);
    }

    private String lookupDdl() {
        try (final var connection = dataSource.getConnection()) {
            final var productName = connection.getMetaData().getDatabaseProductName();
            LOGGER.debug("Identified database as: {}", productName);
            final var ddl = DDL_MAP.get(productName);
            if (ddl == null) {
                throw new IllegalStateException("Unknown database platform: " + productName);
            }
            return ddl;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(getDataSource());
    }

    @Override
    public Stream<String> getContains(final Transaction tx, final FedoraResource fedoraResource) {
        final String transactionId = (tx != null) ? tx.getId() : null;
        final String resourceId = fedoraResource.getFedoraId().getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", resourceId);

        final List<String> children;
        if (transactionId != null) {
            // we are in a transaction
            parameterSource.addValue("transactionId", transactionId);
            children = jdbcTemplate.queryForList(SELECT_CHILDREN_IN_TRANSACTION, parameterSource, String.class);
        } else {
            // not in a transaction
            children = jdbcTemplate.queryForList(SELECT_CHILDREN, parameterSource, String.class);
        }
        LOGGER.debug("getContains for {} in transaction {} found {} children", resourceId, transactionId,
                children.size());
        return children.stream();
    }

    @Override
    public Stream<String> getContainsDeleted(final Transaction tx, final FedoraResource fedoraResource) {
        final String transactionId = (tx != null) ? tx.getId() : null;
        final String resourceId = fedoraResource.getFedoraId().getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", resourceId);

        final List<String> children;
        if (transactionId != null) {
            // we are in a transaction
            parameterSource.addValue("transactionId", transactionId);
            children = jdbcTemplate.queryForList(SELECT_DELETED_CHILDREN_IN_TRANSACTION, parameterSource, String.class);
        } else {
            // not in a transaction
            children = jdbcTemplate.queryForList(SELECT_DELETED_CHILDREN, parameterSource, String.class);
        }
        LOGGER.debug("getContainsDeleted for {} in transaction {} found {} children", resourceId, transactionId,
                children.size());
        return children.stream();
    }

    @Override
    public String getContainedBy(final String txID, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final List<String> parentID;
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_IN_TRANSACTION, parameterSource, String.class);
        } else {
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS, parameterSource, String.class);
        }
        return parentID.stream().findFirst().orElse(null);
    }

    @Override
    public void addContainedBy(@Nonnull final String txID, final FedoraId parent, final FedoraId child) {
        final String parentID = parent.getFullId();
        final String childID = child.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", parentID);
        parameterSource.addValue("child", childID);
        parameterSource.addValue("transactionId", txID);
        final boolean purgedInTxn = !jdbcTemplate.queryForList(IS_CHILD_PURGED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (purgedInTxn) {
            // We purged it, but are re-adding it so remove the purge operation.
            jdbcTemplate.update(UNDO_PURGE_CHILD_IN_TRANSACTION, parameterSource);
        }
        jdbcTemplate.update(INSERT_CHILD_IN_TRANSACTION, parameterSource);
    }

    @Override
    public void removeContainedBy(@Nonnull final String txID, final FedoraId parent, final FedoraId child) {
        final String parentID = parent.getFullId();
        final String childID = child.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", parentID);
        parameterSource.addValue("child", childID);
        parameterSource.addValue("transactionId", txID);
        final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (addedInTxn) {
            jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION, parameterSource);
        } else {
            jdbcTemplate.update(DELETE_CHILD_IN_TRANSACTION, parameterSource);
        }
    }

    @Override
    public void removeResource(@Nonnull final String txID, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        parameterSource.addValue("transactionId", txID);
        final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT,
                parameterSource).isEmpty();
        if (addedInTxn) {
            jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION_NO_PARENT, parameterSource);
        } else {
            final String parent = getContainedBy(txID, resource);
            if (parent != null) {
                LOGGER.debug("Marking containment relationship between parent ({}) and child ({}) deleted", parent,
                        resourceID);
                parameterSource.addValue("parent", parent);
                jdbcTemplate.update(DELETE_CHILD_IN_TRANSACTION, parameterSource);
            }
        }
    }

    @Override
    public void purgeResource(@Nonnull final String txID, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        parameterSource.addValue("transactionId", txID);
        final String parent = getContainedByDeleted(txID, resource);
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
     * @param txID the transaction id.
     * @param resource the child resource id.
     * @return the parent id.
     */
    private String getContainedByDeleted(final String txID, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final List<String> parentID;
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_DELETED_IN_TRANSACTION, parameterSource, String.class);
        } else {
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS_DELETED, parameterSource, String.class);
        }
        return parentID.stream().findFirst().orElse(null);
    }

    @Override
    public void commitTransaction(final Transaction tx) {
        if (tx != null) {
            final String txId = tx.getId();
            final TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("transactionId", txId);
            // Seemingly setting the name ensures that we don't re-use a transaction.
            transactionTemplate.setName("tx-" + txId);
            transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
            transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    protected void doInTransactionWithoutResult(final TransactionStatus status) {
                        try {
                            jdbcTemplate.update(COMMIT_PURGE_RECORDS, parameterSource);
                            jdbcTemplate.update(COMMIT_DELETE_RECORDS, parameterSource);
                            jdbcTemplate.update(COMMIT_ADD_RECORDS, parameterSource);
                            jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, parameterSource);
                        } catch (final Exception e) {
                            status.setRollbackOnly();
                            LOGGER.warn("Unable to commit containment index transaction {}: {}", txId, e.getMessage());
                            throw new RepositoryRuntimeException("Unable to commit containment index transaction", e);
                        }
                    }
                }
            );
        }
    }

    @Override
    public void rollbackTransaction(final Transaction tx) {
        if (tx != null) {
            final String txId = tx.getId();
            final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("transactionId", txId);
            jdbcTemplate.update(DELETE_ENTIRE_TRANSACTION, parameterSource);
        }
    }

    @Override
    public boolean resourceExists(final String txID, final FedoraId fedoraID) {
        // Get the containing ID because fcr:metadata will not exist here but MUST exist if the containing resource does
        final String resourceID = fedoraID.getContainingId();
        LOGGER.debug("Checking if {} exists in transaction {}", resourceID, txID);
        if (fedoraID.isRepositoryRoot()) {
            // Root always exists.
            return true;
        }
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final boolean exists;
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            exists = !jdbcTemplate.queryForList(RESOURCE_EXISTS_IN_TRANSACTION, parameterSource, String.class)
                    .isEmpty();
        } else {
            exists = !jdbcTemplate.queryForList(RESOURCE_EXISTS, parameterSource, String.class).isEmpty();
        }
        return exists;
    }

    @Override
    public FedoraId getContainerIdByPath(final String txID, final FedoraId fedoraId) {
        if (fedoraId.isRepositoryRoot()) {
            // If we are root then we are the top.
            return fedoraId;
        }
        final String parent = getContainedBy(txID, fedoraId);
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
            if (resourceExists(txID, testID)) {
                return testID;
            }
        }
        return FedoraId.getRepositoryRootId();
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
}
