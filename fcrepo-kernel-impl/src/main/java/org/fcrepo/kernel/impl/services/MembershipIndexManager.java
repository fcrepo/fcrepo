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

    private static final String ADD_OPERATION = "add";
    private static final String DELETE_OPERATION = "delete";

    private static final String SELECT_MEMBERSHIP =
            "SELECT property, object_id" +
            " FROM membership" +
            " WHERE subject_id = :subjectId" +
                " AND end_time = :noEndTime";

    private static final String SELECT_MEMBERSHIP_IN_TX =
            "SELECT m.property, m.object_id" +
            " FROM membership m" +
            " WHERE subject_id = :subjectId" +
                " AND end_time = :noEndTime" +
                " AND NOT EXISTS (" +
                    " SELECT 1" +
                    " FROM membership_tx_operations mto" +
                    " WHERE mto.subject_id = :subjectId" +
                        " AND mto.source_id = m.source_id" +
                        " AND mto.object_id = m.object_id" +
                        " AND mto.tx_id = :txId" +
                        " AND mto.operation = :deleteOp)" +
            " UNION" +
            " SELECT property, object_id" +
            " FROM membership_tx_operations" +
            " WHERE subject_id = :subjectId" +
                " AND tx_id = :txId" +
                " AND operation = :addOp";

    private static final String SELECT_MEMBERSHIP_MEMENTO =
            "SELECT property, object_id" +
            " FROM membership" +
            " WHERE subject_id = :subjectId" +
                " AND start_time <= :mementoTime" +
                " AND end_time > :mementoTime";

    private static final String SELECT_MEMBERSHIP_MEMENTO_IN_TX =
            "SELECT property, object_id" +
            " FROM membership m" +
            " WHERE m.subject_id = :subjectId" +
                " AND m.start_time <= :mementoTime" +
                " AND m.end_time > :mementoTime" +
                " AND NOT EXISTS (" +
                    " SELECT 1" +
                    " FROM membership_tx_operations mto" +
                    " WHERE mto.subject_id = :subjectId" +
                        " AND mto.source_id = m.source_id" +
                        " AND mto.property = m.property" +
                        " AND mto.object_id = m.object_id" +
                        " AND mto.end_time <= :mementoTime" +
                        " AND mto.tx_id = :txId" +
                        " AND mto.operation = :deleteOp)" +
            " UNION" +
            " SELECT property, object_id" +
            " FROM membership_tx_operations" +
            " WHERE subject_id = :subjectId" +
                " AND tx_id = :txId" +
                " AND start_time <= :mementoTime" +
                " AND end_time > :mementoTime" +
                " AND operation = :addOp";

    private static final String INSERT_MEMBERSHIP_IN_TX =
            "INSERT INTO membership_tx_operations" +
            " (subject_id, property, object_id, source_id, start_time, end_time, tx_id, operation)" +
            " VALUES (:subjectId, :property, :targetId, :sourceId, :startTime, :endTime, :txId, :operation)";

    private static final String END_EXISTING_MEMBERSHIP =
            "INSERT INTO membership_tx_operations" +
            " (subject_id, property, object_id, source_id, start_time, end_time, tx_id, operation)" +
            " SELECT m.subject_id, m.property, m.object_id, m.source_id, m.start_time, :endTime, :txId, :deleteOp" +
            " FROM membership m" +
            " WHERE m.source_id = :sourceId" +
                " AND m.end_time = :noEndTime" +
                " AND m.subject_id = :subjectId" +
                " AND m.property = :property" +
                " AND m.object_id = :objectId";

    private static final String CLEAR_ENTRY_IN_TX =
            "DELETE FROM membership_tx_operations" +
            " WHERE source_id = :sourceId" +
                " AND tx_id = :txId" +
                " AND subject_id = :subjectId" +
                " AND property = :property" +
                " AND object_id = :objectId" +
                " AND operation = :operation";

    private static final String CLEAR_ALL_ADDED_FOR_SOURCE_IN_TX =
            "DELETE FROM membership_tx_operations" +
            " WHERE source_id = :sourceId" +
                " AND tx_id = :txId" +
                " AND operation = :addOp";

    // Add "delete" entries for all existing membership from the given source, if not already deleted
    private static final String END_EXISTING_FOR_SOURCE =
            "INSERT INTO membership_tx_operations" +
            " (subject_id, property, object_id, source_id, start_time, end_time, tx_id, operation)" +
            " SELECT subject_id, property, object_id, source_id, start_time, :endTime, :txId, :deleteOp" +
            " FROM membership m" +
            " WHERE source_id = :sourceId" +
                " AND end_time = :noEndTime" +
                " AND NOT EXISTS (" +
                    " SELECT TRUE" +
                    " FROM membership_tx_operations mtx" +
                    " WHERE mtx.subject_id = m.subject_id" +
                        " AND mtx.property = m.property" +
                        " AND mtx.object_id = m.object_id" +
                        " AND mtx.source_id = m.source_id" +
                        " AND mtx.operation = :deleteOp" +
                    ")";

    private static final String COMMIT_DELETES =
            "UPDATE membership m" +
            " SET end_time = (" +
                " SELECT mto.end_time" +
                " FROM membership_tx_operations mto" +
                " WHERE mto.tx_id = :txId" +
                    " AND m.source_id = mto.source_id" +
                    " AND m.subject_id = mto.subject_id" +
                    " AND m.property = mto.property" +
                    " AND m.object_id = mto.object_id" +
                " )" +
            " WHERE EXISTS (" +
                "SELECT TRUE" +
                " FROM membership_tx_operations mto" +
                " WHERE mto.tx_id = :txId" +
                    " AND mto.operation = :deleteOp" +
                    " AND m.source_id = mto.source_id" +
                    " AND m.subject_id = mto.subject_id" +
                    " AND m.property = mto.property" +
                    " AND m.object_id = mto.object_id" +
                " )";

    // Transfer all "add" operations from tx to committed membership, unless the entry already exists
    private static final String COMMIT_ADDS =
            "INSERT INTO membership" +
            " (subject_id, property, object_id, source_id, start_time, end_time)" +
            " SELECT subject_id, property, object_id, source_id, start_time, end_time" +
            " FROM membership_tx_operations mto" +
            " WHERE mto.tx_id = :txId" +
                " AND mto.operation = :addOp" +
                " AND NOT EXISTS (" +
                    " SELECT TRUE" +
                    " FROM membership m" +
                    " WHERE m.source_id = mto.source_id" +
                        " AND m.subject_id = mto.subject_id" +
                        " AND m.property = mto.property" +
                        " AND m.object_id = mto.object_id" +
                        " AND m.start_time = mto.start_time" +
                        " AND m.end_time = mto.end_time" +
                " )";

    private static final String DELETE_TRANSACTION =
            "DELETE FROM membership_tx_operations" +
            " WHERE tx_id = :txId";

    private static final String TRUNCATE_MEMBERSHIP = "TRUNCATE TABLE membership";

    private static final String TRUNCATE_MEMBERSHIP_TX = "TRUNCATE TABLE membership_tx_operations";

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(
            DbPlatform.MYSQL, "sql/mysql-membership.sql",
            DbPlatform.H2, "sql/default-membership.sql",
            DbPlatform.POSTGRESQL, "sql/default-membership.sql",
            DbPlatform.MARIADB, "sql/default-membership.sql"
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

    public void deleteMembership(final String txId,  final FedoraId sourceId, final Triple membership,
            final Instant endTime) {
        final Map<String, Object> parameterSource = Map.of(
                "txId", txId,
                "sourceId", sourceId.getFullId(),
                "subjectId", membership.getSubject().getURI(),
                "property", membership.getPredicate().getURI(),
                "objectId", membership.getObject().getURI(),
                "operation", ADD_OPERATION);

        final int affected = jdbcTemplate.update(CLEAR_ENTRY_IN_TX, parameterSource);

        // If no rows were deleted, then assume we need to delete permanent entry
        if (affected == 0) {
            final Map<String, Object> parameterSource2 = Map.of(
                    "txId", txId,
                    "sourceId", sourceId.getFullId(),
                    "subjectId", membership.getSubject().getURI(),
                    "property", membership.getPredicate().getURI(),
                    "objectId", membership.getObject().getURI(),
                    "endTime", Timestamp.from(endTime),
                    "noEndTime", NO_END_TIMESTAMP,
                    "deleteOp", DELETE_OPERATION);
            jdbcTemplate.update(END_EXISTING_MEMBERSHIP, parameterSource2);
        }
    }

    /**
     * Delete all membership properties resulting from the specified source container
     * @param txId transaction id
     * @param sourceId ID of the direct/indirect container whose membership should be cleaned up
     * @param endTime the time the resource was deleted, generally its last modified
     */
    public void deleteMembershipForSource(final String txId, final FedoraId sourceId, final Instant endTime) {
        // End all membership added in this transaction
        final Map<String, Object> parameterSource = Map.of(
                "txId", txId,
                "sourceId", sourceId.getFullId(),
                "addOp", ADD_OPERATION);

        jdbcTemplate.update(CLEAR_ALL_ADDED_FOR_SOURCE_IN_TX, parameterSource);

        // End all membership that existed prior to this transaction
        final Map<String, Object> parameterSource2 = Map.of(
                "txId", txId,
                "sourceId", sourceId.getFullId(),
                "endTime", Timestamp.from(endTime),
                "noEndTime", NO_END_TIMESTAMP,
                "deleteOp", DELETE_OPERATION);
        jdbcTemplate.update(END_EXISTING_FOR_SOURCE, parameterSource2);
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
        // Clear any existing delete operation for this membership
        final Map<String, Object> parametersDelete = Map.of(
                "txId", txId,
                "sourceId", sourceId.getFullId(),
                "subjectId", membership.getSubject().getURI(),
                "property", membership.getPredicate().getURI(),
                "objectId", membership.getObject().getURI(),
                "operation", DELETE_OPERATION);

        jdbcTemplate.update(CLEAR_ENTRY_IN_TX, parametersDelete);

        // Add the new membership operation
        final Map<String, Object> parameterSource = Map.of(
                "subjectId", membership.getSubject().getURI(),
                "property", membership.getPredicate().getURI(),
                "targetId", membership.getObject().getURI(),
                "sourceId", sourceId.getFullId(),
                "startTime", Timestamp.from(startTime),
                "endTime", NO_END_TIMESTAMP,
                "txId", txId,
                "operation", ADD_OPERATION);

        jdbcTemplate.update(INSERT_MEMBERSHIP_IN_TX, parameterSource);
    }

    /**
     * Get a stream of membership triples with
     * @param txId transaction from which membership will be retrieved, or null for no transaction
     * @param subjectId ID of the subject
     * @return Stream of membership triples
     */
    public Stream<Triple> getMembership(final String txId, final FedoraId subjectId) {
        final Node subjectNode = NodeFactory.createURI(subjectId.getBaseId());

        final RowMapper<Triple> membershipMapper = (rs, rowNum) ->
                Triple.create(subjectNode,
                              NodeFactory.createURI(rs.getString("property")),
                              NodeFactory.createURI(rs.getString("object_id")));

        List<Triple> membership = null;
        if (txId == null) {
            if (subjectId.isMemento()) {
                final Map<String, Object> parameterSource = Map.of(
                        "subjectId", subjectId.getBaseId(),
                        "mementoTime", subjectId.getMementoInstant());

                membership = jdbcTemplate.query(SELECT_MEMBERSHIP_MEMENTO, parameterSource, membershipMapper);
            } else {
                final Map<String, Object> parameterSource = Map.of(
                        "subjectId", subjectId.getFullId(),
                        "noEndTime", NO_END_TIMESTAMP);

                membership = jdbcTemplate.query(SELECT_MEMBERSHIP, parameterSource, membershipMapper);
            }
        } else {
            if (subjectId.isMemento()) {
                final Map<String, Object> parameterSource = Map.of(
                        "subjectId", subjectId.getBaseId(),
                        "mementoTime", subjectId.getMementoInstant(),
                        "txId", txId,
                        "addOp", ADD_OPERATION,
                        "deleteOp", DELETE_OPERATION);

                membership = jdbcTemplate.query(SELECT_MEMBERSHIP_MEMENTO_IN_TX, parameterSource, membershipMapper);
            } else {
                final Map<String, Object> parameterSource = Map.of(
                        "subjectId", subjectId.getFullId(),
                        "noEndTime", NO_END_TIMESTAMP,
                        "txId", txId,
                        "addOp", ADD_OPERATION,
                        "deleteOp", DELETE_OPERATION);

                membership = jdbcTemplate.query(SELECT_MEMBERSHIP_IN_TX, parameterSource, membershipMapper);
            }
        }

        return membership.stream();
    }

    /**
     * Perform a commit of operations stored in the specified transaction
     * @param txId transaction id
     */
    public void commitTransaction(final String txId) {
        final Map<String, String> parameterSource = Map.of("txId", txId,
                "addOp", ADD_OPERATION,
                "deleteOp", DELETE_OPERATION);
        final int deletes = jdbcTemplate.update(COMMIT_DELETES, parameterSource);
        final int adds = jdbcTemplate.update(COMMIT_ADDS, parameterSource);
        final int cleaned = jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);

        log.debug("Completed commit, {} deletes, {} adds, {} operations", deletes, adds, cleaned);
    }

    /**
     * Delete all entries related to a transaction
     * @param txId transaction id
     */
    public void deleteTransaction(final String txId) {
        final Map<String, String> parameterSource = Map.of("txId", txId);
        jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);
    }

    /**
     * Clear all entries from the index
     */
    public void clearIndex() {
        jdbcTemplate.update(TRUNCATE_MEMBERSHIP, Map.of());
        jdbcTemplate.update(TRUNCATE_MEMBERSHIP_TX, Map.of());
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
