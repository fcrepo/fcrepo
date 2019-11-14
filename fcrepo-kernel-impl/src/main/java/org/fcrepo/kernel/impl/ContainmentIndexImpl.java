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
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.stream.Stream;

/**
 * @author peichman
 */
public class ContainmentIndexImpl implements ContainmentIndex {
    @Inject
    private DriverManagerDataSource containmentIndexDataSource;

    private Connection conn;

    private PreparedStatement childrenQuery;

    private static final String FEDORA_ID_COLUMN = "fedoraId";

    private static final String SELECT_CHILDREN = "SELECT " + FEDORA_ID_COLUMN + " FROM resources WHERE parent = ?";

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() {
        try {
            conn = containmentIndexDataSource.getConnection();
            childrenQuery = conn.prepareStatement(SELECT_CHILDREN);
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
}
