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

import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.slf4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Stream;

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

    private static final String RESOURCES_TABLE_IDX = "resources_idx";

    private static final String TRANSACTION_OPERATIONS_TABLE = "transactionOperations";

    private static final String TRANSACTION_OPERATIONS_TABLE_IDX_1 = "transactionOperations_idx1";

    private static final String TRANSACTION_OPERATIONS_TABLE_IDX_2 = "transactionOperations_idx2";

    private static final String FEDORA_ID_COLUMN = "fedoraId";

    private static final String PARENT_COLUMN = "parent";

    private static final String TRANSACTION_ID_COLUMN = "transactionId";

    private static final String OPERATION_COLUMN = "operation";

    /**
     * Holds the ID and its parent.
     */
    private static final String RESOURCES_TABLE_DDL = "CREATE TABLE IF NOT EXISTS " + RESOURCES_TABLE + " (" +
            FEDORA_ID_COLUMN + " varchar(65535) NOT NULL PRIMARY KEY, " +
            PARENT_COLUMN + " varchar(65535) NOT NULL)";

    /**
     * Create an index to speed searches for children of a parent.
     */
    private static final String RESOURCES_TABLE_INDEX_DDL = "CREATE INDEX IF NOT EXISTS " + RESOURCES_TABLE_IDX +
            " ON " + RESOURCES_TABLE + " (" + PARENT_COLUMN + ")";

    /**
     * Holds operations to add or delete records from the RESOURCES_TABLE.
     */
    private static final String TRANSACTION_OPERATIONS_TABLE_DDL = "CREATE TABLE IF NOT EXISTS " +
            TRANSACTION_OPERATIONS_TABLE +
            " (" + FEDORA_ID_COLUMN + " varchar(65535) NOT NULL, " +
            PARENT_COLUMN + " varchar(65535) NOT NULL, " +
            TRANSACTION_ID_COLUMN + " varchar(255) NOT NULL, " +
            OPERATION_COLUMN + " varchar(10) NOT NULL)";

    /**
     * Create an index to speed searches for records related to adding/excluding transaction records
     */
    private static final String TRANSACTION_OPERATIONS_TABLE_INDEX_1_DDL = "CREATE INDEX IF NOT EXISTS " +
            TRANSACTION_OPERATIONS_TABLE_IDX_1 + " ON " + TRANSACTION_OPERATIONS_TABLE + " (" + PARENT_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN + ")";

    /**
     * Create an index to speed finding records related to a transaction.
     */
    private static final String TRANSACTION_OPERATIONS_TABLE_INDEX_2_DDL = "CREATE INDEX IF NOT EXISTS " +
            TRANSACTION_OPERATIONS_TABLE_IDX_2 + " ON " + TRANSACTION_OPERATIONS_TABLE + " (" + TRANSACTION_ID_COLUMN +
            ")";

    private static final String SELECT_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent";

    private static final String SELECT_ADDED_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    private static final String SELECT_DELETED_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'delete'";

    private static final String INSERT_CHILD = "INSERT INTO " + RESOURCES_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ") VALUES (:child, :parent)";

    private static final String DELETE_CHILD = "DELETE FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + PARENT_COLUMN + " = :parent";

    private static final String INSERT_CHILD_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN +
            " ) VALUES (:parent, :child, :transactionId, 'add')";

    private static final String UNDO_INSERT_CHILD_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String DELETE_CHILD_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_OPERATIONS_TABLE +
            " ( " + PARENT_COLUMN + ", " + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ", " + OPERATION_COLUMN +
            " ) VALUES (:parent, :child, :transactionId, 'delete')";

    private static final String UNDO_DELETE_CHILD_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    private static final String IS_CHILD_ADDED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + PARENT_COLUMN + " = :parent" +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String IS_CHILD_DELETED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + PARENT_COLUMN + " = :parent" +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    private static final String ROLLBACK_TRANSACTION = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            " transactionId = :transactionId";

    private static final String COMMIT_ADD_RECORDS = "INSERT INTO " + RESOURCES_TABLE + " ( " + FEDORA_ID_COLUMN + ", "
            + PARENT_COLUMN + " ) SELECT " + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + " FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    private static final String COMMIT_DELETE_RECORDS = "DELETE FROM " + RESOURCES_TABLE + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " +  OPERATION_COLUMN + " = 'delete' AND " +
            RESOURCES_TABLE + "." + FEDORA_ID_COLUMN + " = " + TRANSACTION_OPERATIONS_TABLE + "." + FEDORA_ID_COLUMN +
            " AND " + RESOURCES_TABLE + "." + PARENT_COLUMN + " = " + TRANSACTION_OPERATIONS_TABLE + "." +
            PARENT_COLUMN + ")";

    private static final String COMMIT_CLEANUP = "DELETE FROM " + TRANSACTION_OPERATIONS_TABLE + " WHERE " +
            TRANSACTION_ID_COLUMN + " = :transactionId";

    private static final String RESOURCE_EXISTS = "SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child";

    private static final String RESOURCE_EXISTS_ADDITIONS = "SELECT " + FEDORA_ID_COLUMN + " FROM "
            + TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String RESOURCE_EXISTS_DELETIONS = "SELECT " + FEDORA_ID_COLUMN + " FROM "
            + TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    private static final String PARENT_EXISTS = "SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child";

    private static final String PARENT_EXISTS_ADDITIONS = "SELECT " + PARENT_COLUMN + " FROM "
            + TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String PARENT_EXISTS_DELETIONS = "SELECT " + PARENT_COLUMN + " FROM "
            + TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    private static final String DELETE_ALL_RESOURCE = "DELETE FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN +
            " = :child";

    private static final String IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT = "SELECT TRUE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String UNDO_INSERT_CHILD_IN_TRANSACTION_NO_PARENT = "DELETE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN
            + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() {
        jdbcTemplate = getNamedParameterJdbcTemplate();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        // create the tables that don't already exist
        jdbcTemplate.update(RESOURCES_TABLE_DDL, parameterSource);
        jdbcTemplate.update(RESOURCES_TABLE_INDEX_DDL, parameterSource);
        jdbcTemplate.update(TRANSACTION_OPERATIONS_TABLE_DDL, parameterSource);
        jdbcTemplate.update(TRANSACTION_OPERATIONS_TABLE_INDEX_1_DDL, parameterSource);
        jdbcTemplate.update(TRANSACTION_OPERATIONS_TABLE_INDEX_2_DDL, parameterSource);
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(getDataSource());
    }

    /**
     * Do the actual database query to get the contained IDs.
     *
     * @param fedoraId ID of the containing resource
     * @param transactionId ID of the current transaction (if any)
     * @return A stream of contained identifiers
     */
    private Stream<String> getChildren(final String fedoraId, final String transactionId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", fedoraId);

        final List<String> children;
        if (transactionId != null) {
            // we are in a transaction
            parameterSource.addValue("transactionId", transactionId);
            final String currentChildrenQuery = SELECT_CHILDREN +
                    " UNION " + SELECT_ADDED_CHILDREN +
                    " EXCEPT " + SELECT_DELETED_CHILDREN;
            children = jdbcTemplate.queryForList(currentChildrenQuery, parameterSource, String.class);
        } else {
            // not in a transaction
            children = jdbcTemplate.queryForList(SELECT_CHILDREN, parameterSource, String.class);
        }
        LOGGER.debug("getChildren for {} in transaction {} found {} children", fedoraId, transactionId,
                children.size());
        return children.stream();
    }

    @Override
    public Stream<String> getContains(final Transaction tx, final FedoraResource fedoraResource) {
        final String txId = (tx != null) ? tx.getId() : null;
        return getChildren(fedoraResource.getId(), txId);
    }

    @Override
    public String getContainedBy(final String txID, final String resourceID) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final List<String> parentID;
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            final String currentResourceQuery = PARENT_EXISTS +
                    " UNION " + PARENT_EXISTS_ADDITIONS +
                    " EXCEPT " + PARENT_EXISTS_DELETIONS;
            parentID = jdbcTemplate.queryForList(currentResourceQuery, parameterSource, String.class);
        } else {
            parentID = jdbcTemplate.queryForList(PARENT_EXISTS, parameterSource, String.class);
        }
        return parentID.stream().findFirst().orElse(null);
    }

    @Override
    public void addContainedBy(final String txID, final String parentID, final String childID) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", parentID);
        parameterSource.addValue("child", childID);
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            final boolean removedInTxn = !jdbcTemplate.queryForList(IS_CHILD_DELETED_IN_TRANSACTION, parameterSource)
                    .isEmpty();
            if (removedInTxn) {
                jdbcTemplate.update(UNDO_DELETE_CHILD_IN_TRANSACTION, parameterSource);
            } else {
                jdbcTemplate.update(INSERT_CHILD_IN_TRANSACTION, parameterSource);
            }
        } else {
            jdbcTemplate.update(INSERT_CHILD, parameterSource);
        }
    }

    @Override
    public void removeContainedBy(final String txID, final String parentID, final String childID) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", parentID);
        parameterSource.addValue("child", childID);
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION, parameterSource)
                    .isEmpty();
            if (addedInTxn) {
                jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION, parameterSource);
            } else {
                jdbcTemplate.update(DELETE_CHILD_IN_TRANSACTION, parameterSource);
            }
        } else {
            jdbcTemplate.update(DELETE_CHILD, parameterSource);
        }
    }

    @Override
    public void removeResource(final String txID, final String resourceID) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT,
                    parameterSource).isEmpty();
            if (addedInTxn) {
                jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION_NO_PARENT, parameterSource);
            } else {
                final String parent = getContainedBy(txID, resourceID);
                if (parent != null) {
                    LOGGER.debug("Removing containment relationship between parent ({}) and child ({})", parent,
                            resourceID);
                    parameterSource.addValue("parent", parent);
                    jdbcTemplate.update(DELETE_CHILD_IN_TRANSACTION, parameterSource);
                }
            }
        } else {
            jdbcTemplate.update(DELETE_ALL_RESOURCE, parameterSource);
        }
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
                            jdbcTemplate.update(COMMIT_DELETE_RECORDS, parameterSource);
                            jdbcTemplate.update(COMMIT_ADD_RECORDS, parameterSource);
                            jdbcTemplate.update(COMMIT_CLEANUP, parameterSource);
                        } catch (Exception e) {
                            status.setRollbackOnly();
                            LOGGER.warn("Unable to commit containment index transaction: {}", e.getMessage());
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
            jdbcTemplate.update(ROLLBACK_TRANSACTION, parameterSource);
        }
    }

    @Override
    public boolean resourceExists(final String txID, final String resourceID) {
        LOGGER.debug("Checking if {} exists in transaction {}", resourceID, txID);
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        final boolean exists;
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            final String currentResourceQuery = RESOURCE_EXISTS +
                    " UNION " + RESOURCE_EXISTS_ADDITIONS +
                    " EXCEPT " + RESOURCE_EXISTS_DELETIONS;
            exists = !jdbcTemplate.queryForList(currentResourceQuery, parameterSource, String.class).isEmpty();
        } else {
            exists = !jdbcTemplate.queryForList(RESOURCE_EXISTS, parameterSource, String.class).isEmpty();
        }
        return exists;
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
