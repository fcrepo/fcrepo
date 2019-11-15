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
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

/**
 * @author peichman
 */
public class ContainmentIndexImpl implements ContainmentIndex {
    @Inject
    private DriverManagerDataSource containmentIndexDataSource;

    private Connection conn;

    private PreparedStatement childrenQuery;

    private PreparedStatement insertChild;

    private PreparedStatement deleteChild;

    private static final String TABLE_NAME = "resources";

    private static final String FEDORA_ID_COLUMN = "fedoraId";

    private static final String PARENT_COLUMN = "parent";

    private static final String RESOURCES_TABLE_DDL = "CREATE TABLE " + TABLE_NAME + " (" +
            FEDORA_ID_COLUMN + " text PRIMARY KEY, " +
            PARENT_COLUMN + " text);";

    private static final String SELECT_CHILDREN = "SELECT " + FEDORA_ID_COLUMN +
            " FROM " + TABLE_NAME + " WHERE " + PARENT_COLUMN + " = ?";

    private static final String INSERT_CHILD = "INSERT INTO " + TABLE_NAME +
            " (" + FEDORA_ID_COLUMN + ", " + PARENT_COLUMN + ") VALUES (?, ?)";

    private static final String DELETE_CHILD = "DELETE FROM " + TABLE_NAME +
            " WHERE " + FEDORA_ID_COLUMN + " = ? AND " + PARENT_COLUMN + " = ?";

    /**
     * check if the "resources" table exists
     */
    private boolean tableExists() throws SQLException {
        final DatabaseMetaData dbMeta = conn.getMetaData();
        final ResultSet tables = dbMeta.getTables(null, null, "resources", new String[]{"TABLE"});
        while (tables.next()) {
            if (tables.getString("TABLE_NAME").equals("resources")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() {
        try {
            conn = containmentIndexDataSource.getConnection();
            // create the table if it doesn't already exist
            if (!tableExists()) {
                conn.createStatement().execute(RESOURCES_TABLE_DDL);
            }
            childrenQuery = conn.prepareStatement(SELECT_CHILDREN);
            insertChild = conn.prepareStatement(INSERT_CHILD);
            deleteChild = conn.prepareStatement(DELETE_CHILD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Do the actual database query to get the contained IDs.
     *
     * @param fedoraId ID of the containing resource
     * @param transactionId ID of the current transaction (if any)
     * @return A stream of contained identifiers
     */
    private Stream<String> getChildren(final String fedoraId, final String transactionId) {
        // TODO: use the transactionId
        try {
            childrenQuery.setString(1, fedoraId);
            if (childrenQuery.execute()) {
                final Stream.Builder<String> builder = Stream.builder();
                final ResultSet resultSet = childrenQuery.getResultSet();
                while (resultSet.next()) {
                    builder.add(resultSet.getString(FEDORA_ID_COLUMN));
                }
                return builder.build();
            } else {
                return Stream.empty();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
        try {
            insertChild.setString(1, child.getId());
            insertChild.setString(2, parent.getId());
            insertChild.execute();
        } catch (SQLException e) {
            e.printStackTrace();
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
        try {
            deleteChild.setString(1, child.getId());
            deleteChild.setString(2, parent.getId());
            deleteChild.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
