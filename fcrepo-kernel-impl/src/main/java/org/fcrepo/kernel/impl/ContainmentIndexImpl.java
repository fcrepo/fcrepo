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
import org.fcrepo.kernel.api.models.FedoraResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

/**
 * @author peichman
 */
@Component
public class ContainmentIndexImpl implements ContainmentIndex {
    @Inject
    private DataSource dataSource;

    private Connection conn;

    private static final String RESOURCES_TABLE = "resources";

    private static final String TRANSACTION_OPERATIONS_TABLE = "transactionOperations";

    private static final String FEDORA_ID_COLUMN = "fedoraId";

    private static final String PARENT_COLUMN = "parent";

    private static final String TRANSACTION_ID_COLUMN = "transactionId";

    private static final String OPERATION_COLUMN = "operation";

    private static final String RESOURCES_TABLE_DDL = "CREATE TABLE " + RESOURCES_TABLE + " (" +
            FEDORA_ID_COLUMN + " varchar PRIMARY KEY, " +
            PARENT_COLUMN + " varchar);";

    private static final String TRANSACTION_OPERATIONS_TABLE_DDL = "CREATE TABLE " + TRANSACTION_OPERATIONS_TABLE +
            " (" + FEDORA_ID_COLUMN + " varchar PRIMARY KEY, " +
            PARENT_COLUMN + " varchar, " +
            TRANSACTION_ID_COLUMN + " varchar, " +
            OPERATION_COLUMN + " varchar)";

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

    private static final String INSERT_CHILD_IN_TRANSACTION = "INSERT INTO " +
            TRANSACTION_OPERATIONS_TABLE +
            " (parent, fedoraId, transactionId, operation) VALUES (:parent, :child, :transactionId, 'add')";

    private static final String UNDO_INSERT_CHILD_IN_TRANSACTION = "DELETE FROM " +
            TRANSACTION_OPERATIONS_TABLE +
            " WHERE parent = :parent AND fedoraId = :child AND transactionId = :transactionId AND operation = 'add'";

    private static final String DELETE_CHILD_IN_TRANSACTION = "INSERT INTO " +
            TRANSACTION_OPERATIONS_TABLE +
            " (parent, fedoraId, transactionId, operation) VALUES (:parent, :child, :transactionId, 'delete')";

    private final String IS_CHILD_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_OPERATIONS_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :child AND " + PARENT_COLUMN + " = :parent" +
            " AND " + TRANSACTION_ID_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    /**
     * check if a table with the given name exists
     */
    private boolean tableIsMissing(final String tableName) throws SQLException {
        final DatabaseMetaData dbMeta = conn.getMetaData();
        final ResultSet tables = dbMeta.getTables(null, null, tableName, new String[]{"TABLE"});
        while (tables.next()) {
            if (tables.getString("TABLE_NAME").equals(tableName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() throws SQLException {
        conn = dataSource.getConnection();
        // create the tables that don't already exist
        if (tableIsMissing(RESOURCES_TABLE)) {
            conn.createStatement().execute(RESOURCES_TABLE_DDL);
        }
        if (tableIsMissing(TRANSACTION_OPERATIONS_TABLE)) {
            conn.createStatement().execute(TRANSACTION_OPERATIONS_TABLE_DDL);
        }
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
        final NamedParameterJdbcTemplate jdbcTemplate = getNamedParameterJdbcTemplate();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", fedoraId);

        final Stream.Builder<String> children = Stream.builder();
        if (transactionId != null) {
            // we are in a transaction
            parameterSource.addValue("transactionId", transactionId);
            final String currentChildrenQuery = SELECT_CHILDREN +
                    " UNION " + SELECT_ADDED_CHILDREN +
                    " EXCEPT " + SELECT_DELETED_CHILDREN;
            for (final String child : jdbcTemplate.queryForList(currentChildrenQuery, parameterSource, String.class)) {
                children.add(child);
            }
        } else {
            // not in a transaction
            for (final String child : jdbcTemplate.queryForList(SELECT_CHILDREN, parameterSource, String.class)) {
                children.add(child);
            }
        }
        return children.build();
    }

    /**
     * Close the database connection
     */
    @PreDestroy
    private void cleanup() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return a stream of fedora identifiers contained by the specified fedora resource.
     *
     * @param tx The transaction.  If no transaction, null is okay.
     * @param fedoraResource The containing fedora resource
     * @return A stream of contained identifiers
     */
    @Override
    public Stream<String> getContainedBy(final Transaction tx, final FedoraResource fedoraResource) {
        final String txId = (tx != null) ? tx.getId() : null;
        return getChildren(fedoraResource.getId(), txId);
    }

    /**
     * Add a contained by relation between the child resource and its parent.
     *
     * @param tx The transaction.  If no transaction, null is okay.
     * @param parent The containing fedora resource
     * @param child The contained fedora resource
     */
    public void addContainedBy(final Transaction tx, final FedoraResource parent, final FedoraResource child) {
        final String txId = (tx != null) ? tx.getId() : null;
        final NamedParameterJdbcTemplate jdbcTemplate = getNamedParameterJdbcTemplate();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", parent.getId());
        parameterSource.addValue("child", child.getId());
        if (txId != null) {
            parameterSource.addValue("transactionId", txId);
            jdbcTemplate.update(INSERT_CHILD_IN_TRANSACTION, parameterSource);
        } else {
            jdbcTemplate.update(INSERT_CHILD, parameterSource);
        }
    }

    /**
     * Remove a contained by relation between the child resource and its parent.
     *
     * @param tx The transaction.  If no transaction, null is okay.
     * @param parent The containing fedora resource
     * @param child The contained fedora resource
     */
    public void removeContainedBy(final Transaction tx, final FedoraResource parent, final FedoraResource child) {
        final String txId = (tx != null) ? tx.getId() : null;
        final NamedParameterJdbcTemplate jdbcTemplate = getNamedParameterJdbcTemplate();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("parent", parent.getId());
        parameterSource.addValue("child", child.getId());
        if (txId != null) {
            parameterSource.addValue("transactionId", txId);
            final boolean addedInTxn = !jdbcTemplate.queryForMap(IS_CHILD_IN_TRANSACTION, parameterSource).isEmpty();
            if (addedInTxn) {
                jdbcTemplate.update(UNDO_INSERT_CHILD_IN_TRANSACTION, parameterSource);
            } else {
                jdbcTemplate.update(DELETE_CHILD_IN_TRANSACTION, parameterSource);
            }
        } else {
            jdbcTemplate.update(DELETE_CHILD, parameterSource);
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
}
