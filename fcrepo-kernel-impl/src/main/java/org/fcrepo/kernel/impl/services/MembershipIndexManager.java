/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manager for the membership index
 *
 * @author bbpennel
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class MembershipIndexManager {
    private static final Logger log = getLogger(MembershipIndexManager.class);

    private static final Timestamp NO_END_TIMESTAMP = Timestamp.from(MembershipServiceImpl.NO_END_INSTANT);
    private static final Timestamp NO_START_TIMESTAMP = Timestamp.from(Instant.parse("1000-01-01T00:00:00.000Z"));

    private static final String ADD_OPERATION = "add";
    private static final String DELETE_OPERATION = "delete";
    private static final String FORCE_FLAG = "force";

    private static final String TX_ID_PARAM = "txId";
    private static final String SUBJECT_ID_PARAM = "subjectId";
    private static final String OBJECT_ID_PARAM = "objectId";
    private static final String NO_END_TIME_PARAM = "noEndTime";
    private static final String ADD_OP_PARAM = "addOp";
    private static final String DELETE_OP_PARAM = "deleteOp";
    private static final String MEMENTO_TIME_PARAM = "mementoTime";
    private static final String PROPERTY_PARAM = "property";
    private static final String TARGET_ID_PARAM = "targetId";
    private static final String SOURCE_ID_PARAM = "sourceId";
    private static final String PROXY_ID_PARAM = "proxyId";
    private static final String START_TIME_PARAM = "startTime";
    private static final String END_TIME_PARAM = "endTime";
    private static final String LAST_UPDATED_PARAM = "lastUpdated";
    private static final String OPERATION_PARAM = "operation";
    private static final String FORCE_PARAM = "forceFlag";
    private static final String LIMIT_PARAM = "limit";
    private static final String OFFSET_PARAM = "offSet";

    private static final String SELECT_ALL_MEMBERSHIP = "SELECT * FROM membership";

    private static final String SELECT_ALL_OPERATIONS = "SELECT * FROM membership_tx_operations";

    private static final String SELECT_MEMBERSHIP_IN_TX =
            "SELECT property, object_id" +
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
                " AND end_time = :noEndTime" +
                " AND operation = :addOp" +
            " ORDER BY property, object_id" +
            " LIMIT :limit OFFSET :offSet";

    private static final String SELECT_MEMBERSHIP_BY_OBJECT_IN_TX =
            "SELECT subject_id, property" +
                    " FROM membership m" +
                    " WHERE object_id = :objectId" +
                    " AND end_time = :noEndTime" +
                    " AND NOT EXISTS (" +
                        " SELECT 1" +
                        " FROM membership_tx_operations mto" +
                        " WHERE mto.object_id = :objectId" +
                        " AND mto.source_id = m.source_id" +
                        " AND mto.subject_id = m.subject_id" +
                        " AND mto.tx_id = :txId" +
                        " AND mto.operation = :deleteOp)" +
            " UNION" +
            " SELECT subject_id, property" +
                    " FROM membership_tx_operations" +
                    " WHERE object_id = :objectId" +
                    " AND tx_id = :txId" +
                    " AND end_time = :noEndTime" +
                    " AND operation = :addOp" +
                    " ORDER BY subject_id, property" +
                    " LIMIT :limit OFFSET :offSet";

    private static final String DIRECT_SELECT_MEMBERSHIP =
            "SELECT property, object_id" +
            " FROM membership" +
            " WHERE subject_id = :subjectId" +
                " AND end_time = :noEndTime" +
            " ORDER BY property, object_id" +
            " LIMIT :limit OFFSET :offSet";

    private static final String DIRECT_SELECT_MEMBERSHIP_BY_OBJECT =
            "SELECT subject_id, property" +
                    " FROM membership" +
                    " WHERE object_id = :objectId" +
                    " AND end_time = :noEndTime" +
                    " ORDER BY subject_id, property" +
                    " LIMIT :limit OFFSET :offSet";

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
                " AND operation = :addOp" +
            " ORDER BY property, object_id" +
            " LIMIT :limit OFFSET :offSet";

    private static final String SELECT_MEMBERSHIP_BY_OBJECT_MEMENTO_IN_TX =
            "SELECT subject_id, property" +
                    " FROM membership m" +
                    " WHERE m.object_id = :objectId" +
                    " AND m.start_time <= :mementoTime" +
                    " AND m.end_time > :mementoTime" +
                    " AND NOT EXISTS (" +
                        " SELECT 1" +
                        " FROM membership_tx_operations mto" +
                        " WHERE mto.object_id = :objectId" +
                        " AND mto.source_id = m.source_id" +
                        " AND mto.property = m.property" +
                        " AND mto.subject_id = m.subject_id" +
                        " AND mto.end_time <= :mementoTime" +
                        " AND mto.tx_id = :txId" +
                        " AND mto.operation = :deleteOp)" +
            " UNION" +
            " SELECT subject_id, property" +
                    " FROM membership_tx_operations" +
                    " WHERE object_id = :objectId" +
                    " AND tx_id = :txId" +
                    " AND start_time <= :mementoTime" +
                    " AND end_time > :mementoTime" +
                    " AND operation = :addOp" +
                    " ORDER BY subject_id, property" +
                    " LIMIT :limit OFFSET :offSet";

    private static final String DIRECT_SELECT_MEMBERSHIP_MEMENTO =
            "SELECT property, object_id" +
            " FROM membership" +
            " WHERE subject_id = :subjectId" +
                " AND start_time <= :mementoTime" +
                " AND end_time > :mementoTime" +
            " ORDER BY property, object_id" +
            " LIMIT :limit OFFSET :offSet";

    private static final String DIRECT_SELECT_MEMBERSHIP_BY_OBJECT_MEMENTO =
            "SELECT subject_id, property" +
                    " FROM membership" +
                    " WHERE object_id = :objectId" +
                    " AND start_time <= :mementoTime" +
                    " AND end_time > :mementoTime" +
                    " ORDER BY subject_id, property" +
                    " LIMIT :limit OFFSET :offSet";

    private static final String SELECT_LAST_UPDATED =
            "SELECT max(last_updated) as last_updated" +
            " FROM membership" +
            " WHERE subject_id = :subjectId";

    // For mementos, use the start_time instead of last_updated as the
    // end_time reflects when the next version starts
    private static final String SELECT_LAST_UPDATED_MEMENTO =
            "SELECT max(start_time)" +
            " FROM membership" +
            " WHERE subject_id = :subjectId" +
                " AND start_time <= :mementoTime" +
                " AND end_time > :mementoTime";

    private static final String SELECT_LAST_UPDATED_IN_TX =
            "SELECT max(combined.updated) as last_updated" +
            " FROM (" +
                " SELECT max(last_updated) as updated" +
                " FROM membership m" +
                " WHERE subject_id = :subjectId" +
                    " AND NOT EXISTS (" +
                        " SELECT 1" +
                        " FROM membership_tx_operations mto" +
                        " WHERE mto.subject_id = :subjectId" +
                            " AND mto.source_id = m.source_id" +
                            " AND mto.object_id = m.object_id" +
                            " AND mto.tx_id = :txId" +
                            " AND mto.operation = :deleteOp)" +
                " UNION" +
                " SELECT max(last_updated) as updated" +
                " FROM membership_tx_operations" +
                " WHERE subject_id = :subjectId" +
                    " AND tx_id = :txId" +
            ") combined";

    private static final String INSERT_MEMBERSHIP_IN_TX =
            "INSERT INTO membership_tx_operations (subject_id, property, object_id, source_id," +
                    " proxy_id, start_time, end_time, last_updated, tx_id, operation)" +
            " VALUES (:subjectId, :property, :targetId, :sourceId," +
                    " :proxyId, :startTime, :endTime, :lastUpdated, :txId, :operation)";

    private static final String DIRECT_INSERT_MEMBERSHIP =
            "INSERT INTO membership (subject_id, property, object_id, source_id," +
                    " proxy_id, start_time, end_time, last_updated)" +
            " VALUES (:subjectId, :property, :targetId, :sourceId," +
                    " :proxyId, :startTime, :endTime, :lastUpdated)";

    private static final String END_EXISTING_MEMBERSHIP =
            "INSERT INTO membership_tx_operations (subject_id, property, object_id, source_id," +
                    " proxy_id, start_time, end_time, last_updated, tx_id, operation)" +
            " SELECT m.subject_id, m.property, m.object_id, m.source_id, m.proxy_id, m.start_time," +
                    " :endTime, :endTime, :txId, :deleteOp" +
            " FROM membership m" +
            " WHERE m.source_id = :sourceId" +
                " AND m.proxy_id = :proxyId" +
                " AND m.end_time = :noEndTime";

    private static final String DIRECT_END_EXISTING_MEMBERSHIP =
            "UPDATE membership SET end_time = :endTime, last_updated = :endTime" +
            " WHERE source_id = :sourceId" +
                " AND proxy_id = :proxyId" +
                " AND end_time = :noEndTime";

    private static final String CLEAR_FOR_PROXY_IN_TX =
            "DELETE FROM membership_tx_operations" +
            " WHERE source_id = :sourceId" +
                " AND tx_id = :txId" +
                " AND proxy_id = :proxyId" +
                " AND force_flag IS NULL";

    private static final String CLEAR_ALL_ADDED_FOR_SOURCE_IN_TX =
            "DELETE FROM membership_tx_operations" +
            " WHERE source_id = :sourceId" +
                " AND tx_id = :txId" +
                " AND operation = :addOp";

    // Add "delete" entries for all existing membership from the given source, if not already deleted
    private static final String END_EXISTING_FOR_SOURCE =
            "INSERT INTO membership_tx_operations (subject_id, property, object_id, source_id," +
                    " proxy_id, start_time, end_time, last_updated, tx_id, operation)" +
            " SELECT subject_id, property, object_id, source_id," +
                    " proxy_id, start_time, :endTime, :endTime, :txId, :deleteOp" +
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
                        " AND mtx.proxy_id = m.proxy_id" +
                        " AND mtx.operation = :deleteOp" +
                    ")";

    private static final String DIRECT_END_EXISTING_FOR_SOURCE =
            "UPDATE membership SET end_time = :endTime, last_updated = :endTime" +
                    " WHERE source_id = :sourceId" +
                    " AND end_time = :noEndTime";

    private static final String DELETE_EXISTING_FOR_SOURCE_AFTER =
            "INSERT INTO membership_tx_operations(subject_id, property, object_id, source_id," +
                    " proxy_id, start_time, end_time, last_updated, tx_id, operation, force_flag)" +
            " SELECT subject_id, property, object_id, source_id, proxy_id, start_time, end_time," +
                    " last_updated, :txId, :deleteOp, :forceFlag" +
            " FROM membership m" +
            " WHERE m.source_id = :sourceId" +
                " AND (m.start_time >= :startTime" +
                " OR m.end_time >= :startTime)";

    private static final String DIRECT_DELETE_EXISTING_FOR_SOURCE_AFTER =
            "DELETE FROM membership" +
                    " WHERE source_id = :sourceId" +
                    " AND (start_time >= :startTime" +
                    " OR end_time >= :startTime)";

    private static final String DELETE_EXISTING_FOR_PROXY_AFTER =
            "INSERT INTO membership_tx_operations(subject_id, property, object_id, source_id," +
                    " proxy_id, start_time, end_time, last_updated, tx_id, operation, force_flag)" +
            " SELECT subject_id, property, object_id, source_id, proxy_id, start_time, end_time," +
                    " last_updated, :txId, :deleteOp, :forceFlag" +
            " FROM membership m" +
            " WHERE m.proxy_id = :proxyId" +
                " AND (m.start_time >= :startTime" +
                " OR m.end_time >= :startTime)";

    private static final String DIRECT_DELETE_EXISTING_FOR_PROXY_AFTER =
            "UPDATE membership SET end_time = :endTime, last_updated = :endTime" +
            " WHERE proxy_id = :proxyId" +
                " AND (start_time >= :endTime" +
                " OR end_time >= :endTime)";

    private static final String PURGE_ALL_REFERENCES_MEMBERSHIP =
            "DELETE from membership" +
            " where source_id = :targetId" +
                " OR subject_id = :targetId" +
                " OR object_id = :targetId";

    private static final String PURGE_ALL_REFERENCES_TRANSACTION =
            "DELETE from membership_tx_operations" +
            " WHERE tx_id = :txId" +
                " AND (source_id = :targetId" +
                " OR subject_id = :targetId" +
                " OR object_id = :targetId)";

    private static final String COMMIT_DELETES =
            "DELETE from membership" +
            " WHERE EXISTS (" +
                " SELECT TRUE" +
                " FROM membership_tx_operations mto" +
                " WHERE mto.tx_id = :txId" +
                    " AND mto.operation = :deleteOp" +
                    " AND mto.force_flag = :forceFlag" +
                    " AND membership.source_id = mto.source_id" +
                    " AND membership.proxy_id = mto.proxy_id" +
                    " AND membership.subject_id = mto.subject_id" +
                    " AND membership.property = mto.property" +
                    " AND membership.object_id = mto.object_id" +
                " )";

    private static final String COMMIT_ENDS_H2 =
            "UPDATE membership m" +
            " SET end_time = (" +
                " SELECT mto.end_time" +
                " FROM membership_tx_operations mto" +
                " WHERE mto.tx_id = :txId" +
                    " AND m.source_id = mto.source_id" +
                    " AND m.proxy_id = mto.proxy_id" +
                    " AND m.subject_id = mto.subject_id" +
                    " AND m.property = mto.property" +
                    " AND m.object_id = mto.object_id" +
                    " AND mto.operation = :deleteOp" +
                " )," +
                " last_updated = (" +
                    " SELECT mto.end_time" +
                    " FROM membership_tx_operations mto" +
                    " WHERE mto.tx_id = :txId" +
                        " AND m.source_id = mto.source_id" +
                        " AND m.proxy_id = mto.proxy_id" +
                        " AND m.subject_id = mto.subject_id" +
                        " AND m.property = mto.property" +
                        " AND m.object_id = mto.object_id" +
                        " AND mto.operation = :deleteOp" +
                    " )" +
            " WHERE EXISTS (" +
                "SELECT TRUE" +
                " FROM membership_tx_operations mto" +
                " WHERE mto.tx_id = :txId" +
                    " AND mto.operation = :deleteOp" +
                    " AND m.source_id = mto.source_id" +
                    " AND m.proxy_id = mto.proxy_id" +
                    " AND m.subject_id = mto.subject_id" +
                    " AND m.property = mto.property" +
                    " AND m.object_id = mto.object_id" +
                " )";

    private static final String COMMIT_ENDS_POSTGRES =
            "UPDATE membership" +
            " SET end_time = mto.end_time, last_updated = mto.end_time" +
            " FROM membership_tx_operations mto" +
            " WHERE mto.tx_id = :txId" +
                " AND mto.operation = :deleteOp" +
                " AND membership.source_id = mto.source_id" +
                " AND membership.proxy_id = mto.proxy_id" +
                " AND membership.subject_id = mto.subject_id" +
                " AND membership.property = mto.property" +
                " AND membership.object_id = mto.object_id";

    private static final String COMMIT_ENDS_MYSQL =
            "UPDATE membership m" +
            " INNER JOIN membership_tx_operations mto ON" +
                " m.source_id = mto.source_id" +
                " AND m.proxy_id = mto.proxy_id" +
                " AND m.subject_id = mto.subject_id" +
                " AND m.property = mto.property" +
                " AND m.object_id = mto.object_id" +
            " SET m.end_time = mto.end_time, m.last_updated = mto.end_time" +
            " WHERE mto.tx_id = :txId" +
                " AND mto.operation = :deleteOp";

    private static final Map<DbPlatform, String> COMMIT_ENDS_MAP = Map.of(
            DbPlatform.MYSQL, COMMIT_ENDS_MYSQL,
            DbPlatform.MARIADB, COMMIT_ENDS_MYSQL,
            DbPlatform.POSTGRESQL, COMMIT_ENDS_POSTGRES,
            DbPlatform.H2, COMMIT_ENDS_H2
    );

    // Transfer all "add" operations from tx to committed membership, unless the entry already exists
    private static final String COMMIT_ADDS =
            "INSERT INTO membership" +
            " (subject_id, property, object_id, source_id, proxy_id, start_time, end_time, last_updated)" +
            " SELECT subject_id, property, object_id, source_id, proxy_id, start_time, end_time, last_updated" +
            " FROM membership_tx_operations mto" +
            " WHERE mto.tx_id = :txId" +
                " AND mto.operation = :addOp" +
                " AND NOT EXISTS (" +
                    " SELECT TRUE" +
                    " FROM membership m" +
                    " WHERE m.source_id = mto.source_id" +
                        " AND m.proxy_id = mto.proxy_id" +
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

    private DbPlatform dbPlatform;

    private static final int MEMBERSHIP_LIMIT = 50000;

    @PostConstruct
    public void setUp() {
        jdbcTemplate = new NamedParameterJdbcTemplate(getDataSource());
        dbPlatform = DbPlatform.fromDataSource(dataSource);
    }

    /**
     * End a membership from the child of a Direct/IndirectContainer, setting an end time if committed,
     * or clearing from the current tx if it was newly added.
     *
     * @param tx transaction
     * @param sourceId ID of the direct/indirect container whose membership should be ended
     * @param proxyId ID of the proxy producing this membership, when applicable
     * @param endTime the time the resource was deleted, generally its last modified
     */
    public void endMembershipFromChild(final Transaction tx, final FedoraId sourceId, final FedoraId proxyId,
            final Instant endTime) {
        tx.doInTx(() -> {
            if (!tx.isShortLived()) {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue(TX_ID_PARAM, tx.getId());
                parameterSource.addValue(SOURCE_ID_PARAM, sourceId.getFullId());
                parameterSource.addValue(PROXY_ID_PARAM, proxyId.getFullId());

                final int affected = jdbcTemplate.update(CLEAR_FOR_PROXY_IN_TX, parameterSource);

                // If no rows were deleted, then assume we need to delete permanent entry
                if (affected == 0) {
                    final MapSqlParameterSource parameterSource2 = new MapSqlParameterSource();
                    parameterSource2.addValue(TX_ID_PARAM, tx.getId());
                    parameterSource2.addValue(SOURCE_ID_PARAM, sourceId.getFullId());
                    parameterSource2.addValue(PROXY_ID_PARAM, proxyId.getFullId());
                    parameterSource2.addValue(END_TIME_PARAM, formatInstant(endTime));
                    parameterSource2.addValue(NO_END_TIME_PARAM, NO_END_TIMESTAMP);
                    parameterSource2.addValue(DELETE_OP_PARAM, DELETE_OPERATION);
                    jdbcTemplate.update(END_EXISTING_MEMBERSHIP, parameterSource2);
                }
            } else {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue(SOURCE_ID_PARAM, sourceId.getFullId());
                parameterSource.addValue(PROXY_ID_PARAM, proxyId.getFullId());
                parameterSource.addValue(END_TIME_PARAM, formatInstant(endTime));
                parameterSource.addValue(NO_END_TIME_PARAM, NO_END_TIMESTAMP);
                jdbcTemplate.update(DIRECT_END_EXISTING_MEMBERSHIP, parameterSource);
            }
        });
    }

    public void deleteMembershipForProxyAfter(final Transaction tx,
                                              final FedoraId sourceId,
                                              final FedoraId proxyId,
                                              final Instant afterTime) {
        tx.doInTx(() -> {
            final var afterTimestamp = afterTime == null ? NO_START_TIMESTAMP : formatInstant(afterTime);

            if (!tx.isShortLived()) {
                // Clear all membership added in this transaction
                final var parameterSource = Map.of(
                        TX_ID_PARAM, tx.getId(),
                        SOURCE_ID_PARAM, sourceId.getFullId(),
                        PROXY_ID_PARAM, proxyId.getFullId(),
                        OPERATION_PARAM, ADD_OPERATION);

                jdbcTemplate.update(CLEAR_FOR_PROXY_IN_TX, parameterSource);

                // Delete all existing membership entries that start after or end after the given timestamp
                final Map<String, Object> parameterSource2 = Map.of(
                        TX_ID_PARAM, tx.getId(),
                        PROXY_ID_PARAM, proxyId.getFullId(),
                        START_TIME_PARAM, afterTimestamp,
                        FORCE_PARAM, FORCE_FLAG,
                        DELETE_OP_PARAM, DELETE_OPERATION);
                jdbcTemplate.update(DELETE_EXISTING_FOR_PROXY_AFTER, parameterSource2);
            } else {
                final Map<String, Object> parameterSource = Map.of(
                        PROXY_ID_PARAM, proxyId.getFullId(),
                        END_TIME_PARAM, afterTimestamp);
                jdbcTemplate.update(DIRECT_DELETE_EXISTING_FOR_PROXY_AFTER, parameterSource);
            }
        });
    }

    /**
     * End all membership properties resulting from the specified source container
     * @param tx transaction
     * @param sourceId ID of the direct/indirect container whose membership should be ended
     * @param endTime the time the resource was deleted, generally its last modified
     */
    public void endMembershipForSource(final Transaction tx, final FedoraId sourceId, final Instant endTime) {
        tx.doInTx(() -> {
            if (!tx.isShortLived()) {
                final Map<String, Object> parameterSource = Map.of(
                        TX_ID_PARAM, tx.getId(),
                        SOURCE_ID_PARAM, sourceId.getFullId(),
                        ADD_OP_PARAM, ADD_OPERATION);

                jdbcTemplate.update(CLEAR_ALL_ADDED_FOR_SOURCE_IN_TX, parameterSource);

                final Map<String, Object> parameterSource2 = Map.of(
                        TX_ID_PARAM, tx.getId(),
                        SOURCE_ID_PARAM, sourceId.getFullId(),
                        END_TIME_PARAM, formatInstant(endTime),
                        NO_END_TIME_PARAM, NO_END_TIMESTAMP,
                        DELETE_OP_PARAM, DELETE_OPERATION);
                jdbcTemplate.update(END_EXISTING_FOR_SOURCE, parameterSource2);
            } else {
                final Map<String, Object> parameterSource = Map.of(
                        SOURCE_ID_PARAM, sourceId.getFullId(),
                        END_TIME_PARAM, formatInstant(endTime),
                        NO_END_TIME_PARAM, NO_END_TIMESTAMP);
                jdbcTemplate.update(DIRECT_END_EXISTING_FOR_SOURCE, parameterSource);
            }
        });
    }

    /**
     * Delete membership entries that are active at or after the given timestamp for the specified source
     * @param tx transaction
     * @param sourceId ID of the direct/indirect container
     * @param afterTime time at or after which membership should be removed
     */
    public void deleteMembershipForSourceAfter(final Transaction tx, final FedoraId sourceId, final Instant afterTime) {
        tx.doInTx(() -> {
            final var afterTimestamp = afterTime == null ? NO_START_TIMESTAMP : formatInstant(afterTime);

            if (!tx.isShortLived()) {
                // Clear all membership added in this transaction
                final Map<String, Object> parameterSource = Map.of(
                        TX_ID_PARAM, tx.getId(),
                        SOURCE_ID_PARAM, sourceId.getFullId(),
                        ADD_OP_PARAM, ADD_OPERATION);

                jdbcTemplate.update(CLEAR_ALL_ADDED_FOR_SOURCE_IN_TX, parameterSource);

                // Delete all existing membership entries that start after or end after the given timestamp
                final Map<String, Object> parameterSource2 = Map.of(
                        TX_ID_PARAM, tx.getId(),
                        SOURCE_ID_PARAM, sourceId.getFullId(),
                        START_TIME_PARAM, afterTimestamp,
                        FORCE_PARAM, FORCE_FLAG,
                        DELETE_OP_PARAM, DELETE_OPERATION);
                jdbcTemplate.update(DELETE_EXISTING_FOR_SOURCE_AFTER, parameterSource2);
            } else {
                final Map<String, Object> parameterSource = Map.of(
                        SOURCE_ID_PARAM, sourceId.getFullId(),
                        START_TIME_PARAM, afterTimestamp);
                jdbcTemplate.update(DIRECT_DELETE_EXISTING_FOR_SOURCE_AFTER, parameterSource);
            }
        });
    }

    /**
     * Clean up any references to the target id, in transactions and outside
     * @param txId transaction id
     * @param targetId identifier of the resource to cleanup membership references for
     */
    public void deleteMembershipReferences(final String txId, final FedoraId targetId) {
        final Map<String, Object> parameterSource = Map.of(
                TARGET_ID_PARAM, targetId.getFullId(),
                TX_ID_PARAM, txId);

        jdbcTemplate.update(PURGE_ALL_REFERENCES_TRANSACTION, parameterSource);
        jdbcTemplate.update(PURGE_ALL_REFERENCES_MEMBERSHIP, parameterSource);
    }

    /**
     * Add new membership property to the index, clearing any delete
     * operations for the property if necessary.
     * @param tx transaction
     * @param sourceId ID of the direct/indirect container which produced the membership
     * @param proxyId ID of the proxy producing this membership, when applicable
     * @param membership membership triple
     * @param startTime time the membership triple was added
     */
    public void addMembership(final Transaction tx, final FedoraId sourceId, final FedoraId proxyId,
            final Triple membership, final Instant startTime) {
        if (membership == null) {
            return;
        }
        addMembership(tx, sourceId, proxyId, membership, startTime, null);
    }

    /**
     * Add new membership property to the index
     * @param tx transaction
     * @param sourceId ID of the direct/indirect container which produced the membership
     * @param proxyId ID of the proxy producing this membership, when applicable
     * @param membership membership triple
     * @param startTime time the membership triple was added
     * @param endTime time the membership triple ends, or never if not provided
     */
    public void addMembership(final Transaction tx, final FedoraId sourceId, final FedoraId proxyId,
            final Triple membership, final Instant startTime, final Instant endTime) {
        tx.doInTx(() -> {
            final Timestamp endTimestamp;
            final Timestamp lastUpdated;
            final Timestamp startTimestamp = formatInstant(startTime);
            if (endTime == null) {
                endTimestamp = NO_END_TIMESTAMP;
                lastUpdated = startTimestamp;
            } else {
                endTimestamp = formatInstant(endTime);
                lastUpdated = endTimestamp;
            }
            // Add the new membership operation
            final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
            parameterSource.addValue(SUBJECT_ID_PARAM, membership.getSubject().getURI());
            parameterSource.addValue(PROPERTY_PARAM, membership.getPredicate().getURI());
            parameterSource.addValue(TARGET_ID_PARAM, membership.getObject().getURI());
            parameterSource.addValue(SOURCE_ID_PARAM, sourceId.getFullId());
            parameterSource.addValue(PROXY_ID_PARAM, proxyId.getFullId());
            parameterSource.addValue(START_TIME_PARAM, startTimestamp);
            parameterSource.addValue(END_TIME_PARAM, endTimestamp);
            parameterSource.addValue(LAST_UPDATED_PARAM, lastUpdated);

            if (!tx.isShortLived()) {
                parameterSource.addValue(TX_ID_PARAM, tx.getId());
                parameterSource.addValue(OPERATION_PARAM, ADD_OPERATION);
                jdbcTemplate.update(INSERT_MEMBERSHIP_IN_TX, parameterSource);
            } else {
                jdbcTemplate.update(DIRECT_INSERT_MEMBERSHIP, parameterSource);
            }
        });
    }

    /**
     * Get a stream of membership triples with
     * @param tx transaction from which membership will be retrieved, or null for no transaction
     * @param subjectId ID of the subject
     * @return Stream of membership triples
     */
    public Stream<Triple> getMembership(final Transaction tx, final FedoraId subjectId) {
        final Node subjectNode = NodeFactory.createURI(subjectId.getBaseId());

        final RowMapper<Triple> membershipMapper = (rs, rowNum) ->
                Triple.create(subjectNode,
                        NodeFactory.createURI(rs.getString("property")),
                        NodeFactory.createURI(rs.getString("object_id")));

        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        final String query;

        if (subjectId.isMemento()) {
            parameterSource.addValue(SUBJECT_ID_PARAM, subjectId.getBaseId());
            parameterSource.addValue(MEMENTO_TIME_PARAM, formatInstant(subjectId.getMementoInstant()));
        } else {
            parameterSource.addValue(SUBJECT_ID_PARAM, subjectId.getFullId());
            parameterSource.addValue(NO_END_TIME_PARAM, NO_END_TIMESTAMP);
        }

        if (tx.isOpenLongRunning()) {
            parameterSource.addValue(TX_ID_PARAM, tx.getId());

            if (subjectId.isMemento()) {
                query = SELECT_MEMBERSHIP_MEMENTO_IN_TX;
            } else {
                query = SELECT_MEMBERSHIP_IN_TX;
            }
        } else {
            if (subjectId.isMemento()) {
                query = DIRECT_SELECT_MEMBERSHIP_MEMENTO;
            } else {
                query = DIRECT_SELECT_MEMBERSHIP;
            }
        }

        return StreamSupport.stream(new MembershipIterator(query, parameterSource, membershipMapper), false);
    }

    /**
     * Get a stream of membership triples with the given object id as the object
     * @param tx transaction from which membership will be retrieved, or null for no transaction
     * @param objectId ID of the object
     * @return Stream of membership triples
     */
    public Stream<Triple> getMembershipByObject(final Transaction tx, final FedoraId objectId) {
        final Node objectNode = NodeFactory.createURI(objectId.getBaseId());

        final RowMapper<Triple> membershipMapper = (rs, rowNum) ->
                Triple.create(
                        NodeFactory.createURI(rs.getString("subject_id")),
                        NodeFactory.createURI(rs.getString("property")),
                        objectNode);

        final var parameterSource = new MapSqlParameterSource();
        final String query;

        if (objectId.isMemento()) {
            parameterSource.addValue(OBJECT_ID_PARAM, objectId.getBaseId());
            parameterSource.addValue(MEMENTO_TIME_PARAM, formatInstant(objectId.getMementoInstant()));
        } else {
            parameterSource.addValue(OBJECT_ID_PARAM, objectId.getFullId());
            parameterSource.addValue(NO_END_TIME_PARAM, NO_END_TIMESTAMP);
        }

        if (tx.isOpenLongRunning()) {
            parameterSource.addValue(TX_ID_PARAM, tx.getId());

            if (objectId.isMemento()) {
                query = SELECT_MEMBERSHIP_BY_OBJECT_MEMENTO_IN_TX;
            } else {
                query = SELECT_MEMBERSHIP_BY_OBJECT_IN_TX;
            }
        } else {
            if (objectId.isMemento()) {
                query = DIRECT_SELECT_MEMBERSHIP_BY_OBJECT_MEMENTO;
            } else {
                query = DIRECT_SELECT_MEMBERSHIP_BY_OBJECT;
            }
        }

        return StreamSupport.stream(new MembershipIterator(query, parameterSource, membershipMapper), false);
    }

    public Instant getLastUpdated(final Transaction transaction, final FedoraId subjectId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        parameterSource.addValue(NO_END_TIME_PARAM, NO_END_TIMESTAMP);
        final String lastUpdatedQuery;
        if (subjectId.isMemento()) {
            lastUpdatedQuery = SELECT_LAST_UPDATED_MEMENTO;
            parameterSource.addValue(SUBJECT_ID_PARAM, subjectId.getBaseId());
            parameterSource.addValue(MEMENTO_TIME_PARAM, formatInstant(subjectId.getMementoInstant()));
        } else if (transaction.isOpenLongRunning()) {
            lastUpdatedQuery = SELECT_LAST_UPDATED_IN_TX;
            parameterSource.addValue(SUBJECT_ID_PARAM, subjectId.getFullId());
            parameterSource.addValue(TX_ID_PARAM, transaction.getId());
            parameterSource.addValue(DELETE_OP_PARAM, DELETE_OPERATION);
        } else {
            lastUpdatedQuery = SELECT_LAST_UPDATED;
            parameterSource.addValue(SUBJECT_ID_PARAM, subjectId.getFullId());
        }

        final var updated = jdbcTemplate.queryForObject(lastUpdatedQuery, parameterSource, Timestamp.class);
        if (updated != null) {
            return updated.toInstant();
        }
        return null;
    }

    /**
     * Perform a commit of operations stored in the specified transaction
     * @param tx transaction
     */
    public void commitTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            tx.ensureCommitting();
            final Map<String, String> parameterSource = Map.of(
                    TX_ID_PARAM, tx.getId(),
                    ADD_OP_PARAM, ADD_OPERATION,
                    DELETE_OP_PARAM, DELETE_OPERATION,
                    FORCE_PARAM, FORCE_FLAG);

            jdbcTemplate.update(COMMIT_DELETES, parameterSource);
            final int ends = jdbcTemplate.update(COMMIT_ENDS_MAP.get(this.dbPlatform), parameterSource);
            final int adds = jdbcTemplate.update(COMMIT_ADDS, parameterSource);
            final int cleaned = jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);

            log.debug("Completed commit, {} ended, {} adds, {} operations", ends, adds, cleaned);
        }
    }

    /**
     * Delete all entries related to a transaction
     * @param tx transaction
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            final Map<String, String> parameterSource = Map.of(TX_ID_PARAM, tx.getId());
            jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);
        }
    }

    /**
     * Format an instant to a timestamp without milliseconds, due to precision
     * issues with memento datetimes.
     * @param instant the instant
     * @return a Timestamp
     */
    private Timestamp formatInstant(final Instant instant) {
        final var timestamp = Timestamp.from(instant);
        timestamp.setNanos(0);
        return timestamp;
    }

    /**
     * Clear all entries from the index
     */
    public void clearIndex() {
        jdbcTemplate.update(TRUNCATE_MEMBERSHIP, Map.of());
        jdbcTemplate.update(TRUNCATE_MEMBERSHIP_TX, Map.of());
    }

    public void clearAllTransactions() {
        jdbcTemplate.update(TRUNCATE_MEMBERSHIP_TX, Map.of());
    }

    /**
     * Log all membership entries, for debugging usage only
     */
    protected void logMembership() {
        log.info("source_id, proxy_id, subject_id, property, object_id, start_time, end_time, last_updated");
        jdbcTemplate.query(SELECT_ALL_MEMBERSHIP, new RowCallbackHandler() {
            @Override
            public void processRow(final ResultSet rs) throws SQLException {
                log.info("{}, {}, {}, {}, {}, {}, {}, {}",
                        rs.getString("source_id"), rs.getString("proxy_id"), rs.getString("subject_id"),
                        rs.getString("property"), rs.getString("object_id"), rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"), rs.getTimestamp("last_updated"));
            }
        });
    }

    /**
     * Log all membership operations, for debugging usage only
     */
    protected void logOperations() {
        log.info("source_id, proxy_id, subject_id, property, object_id, start_time, end_time,"
                + " last_updated, tx_id, operation, force_flag");
        jdbcTemplate.query(SELECT_ALL_OPERATIONS, new RowCallbackHandler() {
            @Override
            public void processRow(final ResultSet rs) throws SQLException {
                log.info("{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}",
                        rs.getString("source_id"), rs.getString("proxy_id"), rs.getString("subject_id"),
                        rs.getString("property"), rs.getString("object_id"), rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"), rs.getTimestamp("last_updated"), rs.getString("tx_id"),
                        rs.getString("operation"), rs.getString("force_flag"));
            }
        });
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

    /**
     * Private class to back a stream with a paged DB query.
     *
     * If this needs to be run in parallel we will have to override trySplit() and determine a good method to split on.
     */
    private class MembershipIterator extends Spliterators.AbstractSpliterator<Triple> {
        final Queue<Triple> children = new ConcurrentLinkedQueue<>();
        int numOffsets = 0;
        final String queryToUse;
        final MapSqlParameterSource parameterSource;
        final RowMapper<Triple> rowMapper;

        public MembershipIterator(final String query, final MapSqlParameterSource parameters,
                                  final RowMapper<Triple> mapper) {
            super(Long.MAX_VALUE, Spliterator.ORDERED);
            queryToUse = query;
            parameterSource = parameters;
            rowMapper = mapper;
            parameterSource.addValue(ADD_OP_PARAM, ADD_OPERATION);
            parameterSource.addValue(DELETE_OP_PARAM, DELETE_OPERATION);
            parameterSource.addValue(LIMIT_PARAM, MEMBERSHIP_LIMIT);
        }

        @Override
        public boolean tryAdvance(final Consumer<? super Triple> action) {
            try {
                action.accept(children.remove());
            } catch (final NoSuchElementException e) {
                parameterSource.addValue(OFFSET_PARAM, numOffsets * MEMBERSHIP_LIMIT);
                numOffsets += 1;
                children.addAll(jdbcTemplate.query(queryToUse, parameterSource, rowMapper));
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
