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
package org.fcrepo.kernel.impl.services;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.slf4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

/**
 * Manager for the membership index
 *
 * @author bbpennel
 */
@Component
public class MembershipIndexManager {
    private static final Logger log = getLogger(MembershipIndexManager.class);

    private static final Timestamp NO_END_TIMESTAMP = Timestamp.from(Instant.parse("9999-12-31T00:00:00.000Z"));

    private static final String SELECT_MEMBERSHIP =
            "SELECT property, object_id" +
            " FROM membership x" +
            " WHERE subject_id = :subjectId" +
                " AND end_time = :noEndTime";

    private static final String SELECT_MEMBERSHIP_IN_TX =
            "SELECT x.property as property, x.object_id as object_id" +
            " FROM (" +
                " SELECT property, object_id" +
                " FROM membership" +
                " WHERE subject_id = :subjectId" +
                    " AND end_time = :noEndTime" +
                " UNION" +
                " SELECT property, object_id" +
                " FROM membership_tx_operations" +
                " WHERE subject_id = :subjectId" +
                    " AND tx_id = :txId" +
                    " AND operation = 'add'" +
            " ) x" +
            " WHERE NOT EXISTS (" +
                " SELECT 1" +
                " FROM membership_tx_operations" +
                " WHERE subject_id = :subjectId" +
                    " AND operation = 'delete')";

    private static final String SELECT_MEMBERSHIP_MEMENTO =
            "SELECT property, object_id" +
            " FROM membership" +
            " WHERE subject_id = :subjectId" +
                " AND start_time <= :startTime" +
                " AND end_time <= :endTime";

    private static final String INSERT_MEMBERSHIP_IN_TX =
            "INSERT INTO membership_tx_operations" +
            " (subject_id, property, object_id, source_id, start_time, end_time, tx_id, operation)" +
            " VALUES (:subjectId, :property, :targetId, :sourceId, :startTime, :endTime, :txId, :operation)";

    private static final String END_ADDED_FOR_SOURCE_IN_TX =
            "UPDATE membership_tx_operations" +
            " SET end_time = :endTime, operation = 'deleted'" +
            " WHERE source_id = :sourceId" +
                " AND tx_id = :txId" +
                " AND operation = 'added'";

    // Add "deleted" entries for all existing membership from the given source, if not already deleted
    private static final String DELETE_EXISTING_FOR_SOURCE_IN_TX =
            "INSERT INTO membership_tx_operations" +
            " (subject_id, property, object_id, source_id, start_time, end_time, tx_id, operation)" +
            " SELECT subject_id, property, object_id, source_id, start_time, :endTime, :txId, 'deleted'" +
            " FROM membership m" +
            " WHERE source_id = :sourceId" +
                " AND end_time < :noEndTime" +
                " AND NOT EXIST (" +
                    " SELECT TRUE" +
                    " FROM membership_tx_operations mtx" +
                    " WHERE mtx.subject_id = m.fedora_id" +
                        " AND mtx.property = m.property" +
                        " AND mtx.subject_id = m.subject_id" +
                        " AND mtx.source_id = m.source_id" +
                        " AND mtx.operation = 'deleted'" +
                    ")";


    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(
            DbPlatform.MYSQL, "sql/mysql-references.sql",
            DbPlatform.H2, "sql/default-references.sql",
            DbPlatform.POSTGRESQL, "sql/default-references.sql",
            DbPlatform.MARIADB, "sql/default-references.sql"
    );

    @PostConstruct
    public void setUp() {
        jdbcTemplate = new NamedParameterJdbcTemplate(getDataSource());

        final var dbPlatform = DbPlatform.fromDataSource(dataSource);

        Preconditions.checkArgument(DDL_MAP.containsKey(dbPlatform),
                "Missing DDL mapping for %s", dbPlatform);

        final var ddl = DDL_MAP.get(dbPlatform);
        log.debug("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                dataSource);
    }

    /**
     * Delete all membership properties resulting from the specified source container
     * @param txId transaction id
     * @param sourceId ID of the direct/indirect container whose membership should be cleaned up
     */
    public void deleteMembership(final String txId, final FedoraId sourceId, final Instant endTime) {
        // End all membership added in this transaction
        final Map<String, Object> parameterSource = Map.of(
                "txId", txId,
                "sourceId", sourceId.getFullId());

        jdbcTemplate.update(END_ADDED_FOR_SOURCE_IN_TX, parameterSource);

        // End all membership that existed prior to this transaction
        final Map<String, Object> parameterSource2 = Map.of(
                "txId", txId,
                "sourceId", sourceId.getFullId(),
                "endTime", Timestamp.from(endTime),
                "noEndTime", NO_END_TIMESTAMP);
        jdbcTemplate.update(DELETE_EXISTING_FOR_SOURCE_IN_TX, parameterSource2);
    }

    /**
     * Update index with a newly added membership property
     * @param txId transaction id
     * @param sourceId ID of the direct/indirect container which produced the membership
     * @param membership membership triple
     * @param startTime time the membership triple was added
     */
    public void addMembership(final String txId, final FedoraId sourceId, final Triple membership,
            final Instant startTime) {
        final Map<String, Object> parameterSource = Map.of(
                "subjectId", membership.getSubject().getURI(),
                "property", membership.getPredicate().getURI(),
                "targetId", membership.getObject().getURI(),
                "sourceId", sourceId.getFullId(),
                "startTime", Timestamp.from(startTime),
                "endTime", NO_END_TIMESTAMP,
                "txId", txId,
                "operation", "add");

        jdbcTemplate.update(INSERT_MEMBERSHIP_IN_TX, parameterSource);
    }

    public Stream<Triple> getMembership(final String txId, final FedoraId subjectId) {
        final Node subjectNode = NodeFactory.createURI(subjectId.getBaseId());

        final RowMapper<Triple> membershipMapper = (rs, rowNum) ->
                Triple.create(subjectNode,
                              NodeFactory.createURI(rs.getString("property")),
                              NodeFactory.createURI(rs.getString("object_id")));

        List<Triple> membership = null;
        if (txId == null) {
            if (subjectId.isMemento()) {

            } else {
                final Map<String, Object> parameterSource = Map.of(
                        "subjectId", subjectId.getFullId(),
                        "noEndTime", NO_END_TIMESTAMP);

                membership = jdbcTemplate.query(SELECT_MEMBERSHIP, parameterSource, membershipMapper);
            }
        } else {
            if (subjectId.isMemento()) {

            } else {
                final Map<String, Object> parameterSource = Map.of(
                        "subjectId", subjectId.getFullId(),
                        "noEndTime", NO_END_TIMESTAMP,
                        "txId", txId);

                membership = jdbcTemplate.query(SELECT_MEMBERSHIP_IN_TX, parameterSource, membershipMapper);
            }
        }

        return membership.stream();
    }

    /**
     * Set the JDBC datastore.
     * @param dataSource the dataStore.
     */
    public void setDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Get the JDBC datastore.
     * @return the dataStore.
     */
    public DataSource getDataSource() {
        return dataSource;
    }
}
