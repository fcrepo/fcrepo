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

    private static final String SELECT_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent";

    private static final String SELECT_CHILDREN_IN_TRANSACTION = "SELECT x." + FEDORA_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + PARENT_COLUMN + " = :parent" +
            " UNION SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + PARENT_COLUMN + " = :parent AND " + FEDORA_ID_COLUMN + " = x." + FEDORA_ID_COLUMN +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete')";

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
            TRANSACTION_ID_COLUMN + " = :transactionId";

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

    private static final String RESOURCE_EXISTS_IN_TRANSACTION = "SELECT " + FEDORA_ID_COLUMN + " FROM" +
            " (SELECT " + FEDORA_ID_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            " UNION SELECT " + FEDORA_ID_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'delete')";

    private static final String PARENT_EXISTS = "SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child";

    private static final String PARENT_EXISTS_IN_TRANSACTION= "SELECT x." + PARENT_COLUMN + " FROM" +
            " (SELECT " + PARENT_COLUMN + " FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child" +
            " UNION SELECT " + PARENT_COLUMN + " FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'add') x" +
            " WHERE NOT EXISTS " +
            " (SELECT 1 FROM " + TRANSACTION_OPERATIONS_TABLE+
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + TRANSACTION_ID_COLUMN + " = :transactionId" +
            " AND " + OPERATION_COLUMN + " = 'delete')";

    private static final String DELETE_ALL_RESOURCE = "DELETE FROM " + RESOURCES_TABLE + " WHERE " + FEDORA_ID_COLUMN +
            " = :child";

    private static final String IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT = "SELECT TRUE FROM " +
            TRANSACTION_OPERATIONS_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :child AND " +
            TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

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
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                dataSource);
    }

    private String lookupDdl() {
        try (var connection = dataSource.getConnection()) {
            final var productName = connection.getMetaData().getDatabaseProductName();
            final var ddl = DDL_MAP.get(productName);
            if (ddl == null) {
                throw new IllegalStateException("Unknown database platform: " + productName);
            }
            return ddl;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(getDataSource());
    }

    /**
     * Do the actual database query to get the contained IDs.
     *
     * @param resource Containing resource
     * @param transactionId ID of the current transaction (if any)
     * @return A stream of contained identifiers
     */
    private Stream<String> getChildren(final FedoraResource resource, final String transactionId) {
        final String resourceId = resource.getFedoraId().getFullId();
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
        LOGGER.debug("getChildren for {} in transaction {} found {} children", resourceId, transactionId,
                children.size());
        return children.stream();
    }

    @Override
    public Stream<String> getContains(final Transaction tx, final FedoraResource fedoraResource) {
        final String txId = (tx != null) ? tx.getId() : null;
        return getChildren(fedoraResource, txId);
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
    public void addContainedBy(final String txID, final FedoraId parent, final FedoraId child) {
        final String parentID = parent.getFullId();
        final String childID = child.getFullId();
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
    public void removeContainedBy(final String txID, final FedoraId parent, final FedoraId child) {
        final String parentID = parent.getFullId();
        final String childID = child.getFullId();
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
    public void removeResource(final String txID, final FedoraId resource) {
        final String resourceID = resource.getFullId();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("child", resourceID);
        if (txID != null) {
            parameterSource.addValue("transactionId", txID);
            final boolean addedInTxn = !jdbcTemplate.queryForList(IS_CHILD_ADDED_IN_TRANSACTION_NO_PARENT,
                    parameterSource).isEmpty();
            if (addedInTxn) {
                jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION_NO_PARENT, parameterSource);
            } else {
                final String parent = getContainedBy(txID, resource);
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
            jdbcTemplate.update(ROLLBACK_TRANSACTION, parameterSource);
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
