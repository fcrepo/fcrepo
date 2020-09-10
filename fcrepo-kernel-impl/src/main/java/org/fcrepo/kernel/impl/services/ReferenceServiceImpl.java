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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.slf4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Implementation of reference service.
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class ReferenceServiceImpl implements ReferenceService {

    private static final Logger LOGGER = getLogger(ReferenceServiceImpl.class);

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "reference";

    private static final String TRANSACTION_TABLE = "reference_transaction_operations";

    private static final String SUBJECT_COLUMN = "fedora_id";

    private static final String PROPERTY_COLUMN = "property";

    private static final String TARGET_COLUMN = "target_id";

    private static final String OPERATION_COLUMN = "operation";

    private static final String TRANSACTION_COLUMN = "transaction_id";

    private static final String SELECT_INBOUND = "SELECT " + SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + " FROM " +
            TABLE_NAME + " WHERE " + TARGET_COLUMN + " = :targetId";

    private static final String SELECT_INBOUND_IN_TRANSACTION = "SELECT x." + SUBJECT_COLUMN + ", x." +
            PROPERTY_COLUMN + " FROM " + "(SELECT " + SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + " FROM " + TABLE_NAME +
            " WHERE " + TARGET_COLUMN + " = :targetId UNION " + "SELECT " + SUBJECT_COLUMN + ", " + PROPERTY_COLUMN +
            " FROM " + TRANSACTION_TABLE + " WHERE " + TARGET_COLUMN + " = :targetId AND "
            + TRANSACTION_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add') x WHERE NOT EXISTS " +
            "(SELECT 1 FROM " + TRANSACTION_TABLE + " WHERE " + TARGET_COLUMN + " = :targetId AND " +
            OPERATION_COLUMN + " = 'delete')";

    private static final String SELECT_OUTBOUND = "SELECT " + TARGET_COLUMN + ", " + PROPERTY_COLUMN + " FROM " +
            TABLE_NAME + " WHERE " + SUBJECT_COLUMN + " = :resourceId";

    private static final String SELECT_OUTBOUND_IN_TRANSACTION = "SELECT x." + TARGET_COLUMN + ", x." +
            PROPERTY_COLUMN + " FROM " + "(SELECT " + TARGET_COLUMN + ", " + PROPERTY_COLUMN + " FROM " + TABLE_NAME +
            " WHERE " + SUBJECT_COLUMN + " = :resourceId UNION " + "SELECT " + TARGET_COLUMN + ", " + PROPERTY_COLUMN +
            " FROM " + TRANSACTION_TABLE + " WHERE " + SUBJECT_COLUMN + " = :resourceId " +
            "AND " + TRANSACTION_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add') x WHERE NOT " +
            "EXISTS (SELECT 1 FROM " + TRANSACTION_TABLE + " WHERE " + SUBJECT_COLUMN + " = :resourceId AND " +
            OPERATION_COLUMN + " = 'delete')";

    private static final String INSERT_REFERENCE_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_TABLE + "(" +
            SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " + TARGET_COLUMN + ", " + TRANSACTION_COLUMN + ", " +
            OPERATION_COLUMN + ") VALUES (:resourceId, :property, :targetId, :transactionId, 'add')";

    private static final String UNDO_INSERT_REFERENCE_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_TABLE + " WHERE " +
            SUBJECT_COLUMN + " = :resourceId AND " + PROPERTY_COLUMN + " = :property AND " + TARGET_COLUMN +
            " = :targetId AND " + TRANSACTION_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String DELETE_REFERENCE_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_TABLE + "(" +
            SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " + TARGET_COLUMN + ", " + TRANSACTION_COLUMN + ", " +
            OPERATION_COLUMN + ") VALUES (:resourceId, :property, :targetId, :transactionId, 'delete')";

    private static final String UNDO_DELETE_REFERENCE_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_TABLE + " WHERE " +
            SUBJECT_COLUMN + " = :resourceId AND " + PROPERTY_COLUMN + " = :property AND " + TARGET_COLUMN +
            " = :targetId AND " + TRANSACTION_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    private static final String IS_REFERENCE_ADDED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_TABLE + " WHERE "
            + SUBJECT_COLUMN + " = :resourceId AND " + PROPERTY_COLUMN + " = :property AND " + TARGET_COLUMN +
            " = :targetId AND " + TRANSACTION_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String IS_REFERENCE_DELETED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_TABLE +
            " WHERE " + SUBJECT_COLUMN + " = :resourceId AND " + PROPERTY_COLUMN + " = :property AND " + TARGET_COLUMN +
            " = :targetId AND " + TRANSACTION_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    private static final String COMMIT_ADD_RECORDS = "INSERT INTO " + TABLE_NAME + " ( " + SUBJECT_COLUMN + ", "
            + PROPERTY_COLUMN + ", " + TARGET_COLUMN + " ) SELECT " + SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " +
            TARGET_COLUMN + " FROM " + TRANSACTION_TABLE + " WHERE " + TRANSACTION_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    private static final String COMMIT_DELETE_RECORDS = "DELETE FROM " + TABLE_NAME + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_TABLE + " t WHERE t." +
            TRANSACTION_COLUMN + " = :transactionId AND t." +  OPERATION_COLUMN + " = 'delete' AND" +
            " t." + SUBJECT_COLUMN + " = " + TABLE_NAME + "." + SUBJECT_COLUMN +
            " AND t." + PROPERTY_COLUMN + " = " + TABLE_NAME + "." + PROPERTY_COLUMN +
            " AND t." + TARGET_COLUMN + " = " + TABLE_NAME + "." + TARGET_COLUMN + ")";

    private static final String DELETE_TRANSACTION = "DELETE FROM " + TRANSACTION_TABLE + " WHERE " +
            TRANSACTION_COLUMN + " = :transactionId";

    private static final String TRUNCATE_TABLE = "TRUNCATE TABLE " + TABLE_NAME;

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
        LOGGER.info("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                dataSource);
    }

    @Override
    public RdfStream getInboundReferences(final String txId, final FedoraResource resource) {
        final String resourceId = resource.getFedoraId().getFullId();
        final Node subject = NodeFactory.createURI(resourceId);
        final Stream<Triple> stream = getReferencesInternal(txId, resourceId);
        if (resource instanceof NonRdfSourceDescription) {
            final Stream<Triple> stream2 = getReferencesInternal(txId, resource.getFedoraId().getBaseId());
            return new DefaultRdfStream(subject, Stream.concat(stream, stream2));
        }
        return new DefaultRdfStream(subject, stream);
    }

    /**
     * Get the inbound references for the resource Id and the transaction id.
     * @param txId transaction id or null for none.
     * @param resourceId the resource id.
     * @return RDF stream of inbound references
     */
    private Stream<Triple> getReferencesInternal(final String txId, final String resourceId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("targetId", resourceId);
        final Node targetNode = NodeFactory.createURI(resourceId);

        final RowMapper<Triple> inboundMapper = (rs, rowNum) ->
                Triple.create(NodeFactory.createURI(rs.getString(SUBJECT_COLUMN)),
                        NodeFactory.createURI(rs.getString(PROPERTY_COLUMN)),
                        targetNode);

        final List<Triple> references;
        if (txId != null) {
            // we are in a transaction
            parameterSource.addValue("transactionId", txId);
            references = jdbcTemplate.query(SELECT_INBOUND_IN_TRANSACTION, parameterSource, inboundMapper);
        } else {
            // not in a transaction
            references = jdbcTemplate.query(SELECT_INBOUND, parameterSource, inboundMapper);
        }
        LOGGER.debug("getInboundReferences for {} in transaction {} found {} references",
                resourceId, txId, references.size());
        return references.stream();
    }

    @Override
    public void deleteAllReferences(@Nonnull final String txId, final FedoraId resourceId) {
        final Stream<Triple> deleteReferences = getOutboundReferences(txId, resourceId);
        // Remove all the existing references.
        deleteReferences.forEach(t ->
                removeReference(txId, resourceId.getFullId(), t.getPredicate().getURI(), t.getObject().getURI())
        );
    }

    /**
     * Get a stream of triples of resources being referenced from the provided resource.
     * @param txId transaction Id or null if none.
     * @param resourceId the resource Id.
     * @return stream of Triples
     */
    private Stream<Triple> getOutboundReferences(final String txId, final FedoraId resourceId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("resourceId", resourceId.getFullId());
        final Node subjectNode = NodeFactory.createURI(resourceId.getFullId());

        final RowMapper<Triple> outboundMapper = (rs, rowNum) ->
                Triple.create(subjectNode,
                        NodeFactory.createURI(rs.getString(PROPERTY_COLUMN)),
                        NodeFactory.createURI(rs.getString(TARGET_COLUMN)));

        final List<Triple> references;
        if (txId != null) {
            // we are in a transaction
            parameterSource.addValue("transactionId", txId);
            references = jdbcTemplate.query(SELECT_OUTBOUND_IN_TRANSACTION, parameterSource, outboundMapper);
        } else {
            // not in a transaction
            references = jdbcTemplate.query(SELECT_OUTBOUND, parameterSource, outboundMapper);
        }
        LOGGER.debug("getOutboundReferences for {} in transaction {} found {} references",
                resourceId, txId, references.size());
        return references.stream();
    }

    @Override
    @Transactional
    public void updateReferences(@Nonnull final String txId, final FedoraId resourceId, final RdfStream rdfStream) {
        try {
            final Stream<Triple> deleteReferences = getOutboundReferences(txId, resourceId);
            // Remove all the existing references.
            deleteReferences.forEach(t ->
                removeReference(txId, resourceId.getFullId(), t.getPredicate().getURI(), t.getObject().getURI())
            );
            final Stream<Triple> addReferences = getReferencesFromRdf(rdfStream);
            addReferences.forEach(r -> addReference(txId, resourceId.getFullId(),
                    r.getPredicate().getURI(), r.getObject().getURI()));
        } catch (final Exception e) {
            LOGGER.warn("Unable to update reference index for resource {} in transaction {}: {}",
                    resourceId.getFullId(), txId, e.getMessage());
            throw new RepositoryRuntimeException("Unable to update reference index", e);
        }
    }

    @Override
    @Transactional
    public void commitTransaction(final String txId) {
        try {
            final Map<String, String> parameterSource = Map.of("transactionId", txId);
            jdbcTemplate.update(COMMIT_DELETE_RECORDS, parameterSource);
            jdbcTemplate.update(COMMIT_ADD_RECORDS, parameterSource);
            jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);
        } catch (final Exception e) {
            LOGGER.warn("Unable to commit reference index transaction {}: {}", txId, e.getMessage());
            throw new RepositoryRuntimeException("Unable to commit reference index transaction", e);
        }
    }

    @Override
    @Transactional
    public void rollbackTransaction(final String txId) {
        try {
            final Map<String, String> parameterSource = Map.of("transactionId", txId);
            jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);
        } catch (final Exception e) {
            LOGGER.warn("Unable to rollback reference index transaction {}: {}", txId, e.getMessage());
            throw new RepositoryRuntimeException("Unable to rollback reference index transaction", e);
        }
    }

    @Override
    public void reset() {
        try {
            jdbcTemplate.update(TRUNCATE_TABLE, Map.of());
        } catch (final Exception e) {
            LOGGER.warn("Unable to reset reference index: {}", e.getMessage());
            throw new RepositoryRuntimeException("Unable to reset reference index", e);
        }
    }

    /**
     * Remove a reference.
     * @param txId transaction Id.
     * @param resourceId the subject resource Id.
     * @param targetId the target resource Id.
     */
    private void removeReference(@Nonnull final String txId, final String resourceId, final String property,
                                 final String targetId) {
        final Map<String, String> parameterSource = Map.of("transactionId", txId,
                "resourceId", resourceId,
                "property", property,
                "targetId", targetId);
        final boolean addedInTx = !jdbcTemplate.queryForList(IS_REFERENCE_ADDED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (addedInTx) {
            jdbcTemplate.update(UNDO_INSERT_REFERENCE_IN_TRANSACTION, parameterSource);
        } else {
            jdbcTemplate.update(DELETE_REFERENCE_IN_TRANSACTION, parameterSource);
        }
    }

    /**
     * Add a reference
     * @param txId the transaction Id.
     * @param resourceId the subject resource Id.
     * @param targetId the target resource Id.
     */
    private void addReference(@Nonnull final String txId, final String resourceId, final String property,
                              final String targetId) {
        final Map<String, String> parameterSource = Map.of("transactionId", txId,
                "resourceId", resourceId,
                "property", property,
                "targetId", targetId);
        final boolean addedInTx = !jdbcTemplate.queryForList(IS_REFERENCE_DELETED_IN_TRANSACTION, parameterSource)
                .isEmpty();
        if (addedInTx) {
            jdbcTemplate.update(UNDO_DELETE_REFERENCE_IN_TRANSACTION, parameterSource);
        } else {
            jdbcTemplate.update(INSERT_REFERENCE_IN_TRANSACTION, parameterSource);
        }
    }

    /**
     * Utility to filter a RDFStream to just the URIs from subjects and objects within the repository.
     * @param stream the provided stream
     * @return stream of triples with internal references.
     */
    private Stream<Triple> getReferencesFromRdf(final RdfStream stream) {
        final Predicate<Triple> isInternalReference = t -> {
            final Node s = t.getSubject();
            final Node o = t.getObject();
            return (s.isURI() && s.getURI().startsWith(FEDORA_ID_PREFIX) && o.isURI() &&
                    o.getURI().startsWith(FEDORA_ID_PREFIX));
        };
        return stream.peek(t -> LOGGER.trace("Before reference filtering: {}", t)).filter(isInternalReference)
                .peek(t -> LOGGER.trace("After reference filtering: {}", t));
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
