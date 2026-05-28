/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.kernel.impl.operations.ReferenceOperation;
import org.fcrepo.kernel.impl.operations.ReferenceOperationBuilder;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of reference service.
 * @author whikloj
 * @since 6.0.0
 */
@Component("referenceServiceImpl")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ReferenceServiceImpl implements ReferenceService {

    private static final Logger LOGGER = getLogger(ReferenceServiceImpl.class);

    @Inject
    private DataSource dataSource;

    @Inject
    private EventAccumulator eventAccumulator;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Inject
    private RepositoryInitializationStatus initializationStatus;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "reference";

    private static final String TRANSACTION_TABLE = "reference_transaction_operations";

    private static final String RESOURCE_COLUMN = "fedora_id";

    private static final String SUBJECT_COLUMN = "subject_id";

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

    private static final String SELECT_OUTBOUND = "SELECT " + SUBJECT_COLUMN + ", " + TARGET_COLUMN + ", " +
            PROPERTY_COLUMN + " FROM " + TABLE_NAME + " WHERE " + RESOURCE_COLUMN + " = :resourceId";

    private static final String SELECT_OUTBOUND_IN_TRANSACTION = "SELECT x." + SUBJECT_COLUMN + ", x." + TARGET_COLUMN +
            ", x." + PROPERTY_COLUMN + " FROM " + "(SELECT " + SUBJECT_COLUMN + ", " + TARGET_COLUMN + ", " +
            PROPERTY_COLUMN + " FROM " + TABLE_NAME + " WHERE " + RESOURCE_COLUMN + " = :resourceId UNION " +
            "SELECT " + SUBJECT_COLUMN + ", " + TARGET_COLUMN + ", " + PROPERTY_COLUMN + " FROM " + TRANSACTION_TABLE +
            " WHERE " + RESOURCE_COLUMN + " = :resourceId " + "AND " + TRANSACTION_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add') x WHERE NOT EXISTS (SELECT 1 FROM " + TRANSACTION_TABLE + " WHERE " +
            RESOURCE_COLUMN + " = :resourceId AND " + OPERATION_COLUMN + " = 'delete')";

    private static final String INSERT_REFERENCE_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_TABLE + "(" +
            RESOURCE_COLUMN + ", " + SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " + TARGET_COLUMN + ", " +
            TRANSACTION_COLUMN + ", " + OPERATION_COLUMN + ") VALUES (:resourceId, :subjectId, :property, :targetId, " +
            ":transactionId, 'add')";

    private static final String INSERT_REFERENCE_DIRECT = "INSERT INTO " + TABLE_NAME + "(" +
            RESOURCE_COLUMN + ", " + SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " + TARGET_COLUMN +
            ") VALUES (:resourceId, :subjectId, :property, :targetId)";

    private static final String UNDO_INSERT_REFERENCE_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_TABLE + " WHERE " +
            RESOURCE_COLUMN + " = :resourceId AND " + SUBJECT_COLUMN + " = :subjectId AND " + PROPERTY_COLUMN +
            " = :property AND " + TARGET_COLUMN + " = :targetId AND " + TRANSACTION_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    private static final String DELETE_REFERENCE_IN_TRANSACTION = "INSERT INTO " + TRANSACTION_TABLE + "(" +
            RESOURCE_COLUMN + ", " + SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " + TARGET_COLUMN + ", " +
            TRANSACTION_COLUMN + ", " + OPERATION_COLUMN + ") VALUES (:resourceId, :subjectId, :property, :targetId, " +
            ":transactionId, 'delete')";

    private static final String DELETE_REFERENCE_DIRECT = "DELETE FROM reference" +
            " WHERE fedora_id = :resourceId AND subject_id = :subjectId" +
            " AND property = :property AND target_id = :targetId";

    private static final String UNDO_DELETE_REFERENCE_IN_TRANSACTION = "DELETE FROM " + TRANSACTION_TABLE + " WHERE " +
            RESOURCE_COLUMN + " = :resourceId AND " + SUBJECT_COLUMN + " = :subjectId AND " + PROPERTY_COLUMN +
            " = :property AND " + TARGET_COLUMN + " = :targetId AND " + TRANSACTION_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'delete'";

    private static final String IS_REFERENCE_ADDED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_TABLE + " WHERE "
            + RESOURCE_COLUMN + " = :resourceId AND " + SUBJECT_COLUMN + " = :subjectId AND " + PROPERTY_COLUMN +
            " = :property AND " + TARGET_COLUMN + " = :targetId AND " + TRANSACTION_COLUMN + " = :transactionId AND " +
            OPERATION_COLUMN + " = 'add'";

    private static final String IS_REFERENCE_DELETED_IN_TRANSACTION = "SELECT TRUE FROM " + TRANSACTION_TABLE +
            " WHERE " + RESOURCE_COLUMN + " = :resourceId AND " + SUBJECT_COLUMN + " = :subjectId AND " +
            PROPERTY_COLUMN + " = :property AND " + TARGET_COLUMN + " = :targetId AND " + TRANSACTION_COLUMN +
            " = :transactionId AND " + OPERATION_COLUMN + " = 'delete'";

    private static final String COMMIT_ADD_RECORDS = "INSERT INTO " + TABLE_NAME + " ( " + RESOURCE_COLUMN + ", " +
            SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " + TARGET_COLUMN + " ) SELECT " + RESOURCE_COLUMN + ", " +
            SUBJECT_COLUMN + ", " + PROPERTY_COLUMN + ", " + TARGET_COLUMN + " FROM " + TRANSACTION_TABLE + " WHERE " +
            TRANSACTION_COLUMN + " = :transactionId AND " + OPERATION_COLUMN + " = 'add'";

    private static final String COMMIT_DELETE_RECORDS = "DELETE FROM " + TABLE_NAME + " WHERE " +
            "EXISTS (SELECT * FROM " + TRANSACTION_TABLE + " t WHERE t." +
            TRANSACTION_COLUMN + " = :transactionId AND t." +  OPERATION_COLUMN + " = 'delete' AND" +
            " t." + RESOURCE_COLUMN + " = " + TABLE_NAME + "." + RESOURCE_COLUMN + " AND" +
            " t." + SUBJECT_COLUMN + " = " + TABLE_NAME + "." + SUBJECT_COLUMN +
            " AND t." + PROPERTY_COLUMN + " = " + TABLE_NAME + "." + PROPERTY_COLUMN +
            " AND t." + TARGET_COLUMN + " = " + TABLE_NAME + "." + TARGET_COLUMN + ")";
    private static final String COMMIT_DELETE_RECORD_POSTGRES = "DELETE FROM " + TABLE_NAME + " r" +
            " USING " + TRANSACTION_TABLE + " rto" +
            " WHERE r." + RESOURCE_COLUMN + " = rto." + RESOURCE_COLUMN +
            " AND r." + SUBJECT_COLUMN + " = rto." + SUBJECT_COLUMN +
            " AND r." + PROPERTY_COLUMN + " = rto." + PROPERTY_COLUMN +
            " AND r." + TARGET_COLUMN + " = rto." + TARGET_COLUMN +
            " AND rto." + TRANSACTION_COLUMN + " = :transactionId" +
            " AND rto." + OPERATION_COLUMN + " = 'delete'";
    private static final String COMMIT_DELETE_RECORD_MYSQL = "DELETE r" +
            " FROM " + TABLE_NAME + " r" +
            " INNER JOIN " + TRANSACTION_TABLE + " rto" +
            " ON r." + RESOURCE_COLUMN + " = rto." + RESOURCE_COLUMN +
            " AND r." + SUBJECT_COLUMN + " = rto." + SUBJECT_COLUMN +
            " AND r." + PROPERTY_COLUMN + " = rto." + PROPERTY_COLUMN +
            " AND r." + TARGET_COLUMN + " = rto." + TARGET_COLUMN +
            " WHERE rto." + TRANSACTION_COLUMN + " = :transactionId" +
            " AND rto." + OPERATION_COLUMN + " = 'delete'";
    private static final Map<DbPlatform, String> COMMIT_DELETE_RECORD_MAP = Map.of(
            DbPlatform.POSTGRESQL, COMMIT_DELETE_RECORD_POSTGRES,
            DbPlatform.MYSQL, COMMIT_DELETE_RECORD_MYSQL,
            DbPlatform.MARIADB, COMMIT_DELETE_RECORD_MYSQL,
            DbPlatform.H2, COMMIT_DELETE_RECORDS
            );

    private static final String DELETE_TRANSACTION = "DELETE FROM " + TRANSACTION_TABLE + " WHERE " +
            TRANSACTION_COLUMN + " = :transactionId";

    private static final String TRUNCATE_TABLE = "TRUNCATE TABLE " + TABLE_NAME;
    private static final String TRUNCATE_TX_TABLE = "TRUNCATE TABLE " + TRANSACTION_TABLE;

    private DbPlatform dbPlatform;

    @PostConstruct
    public void setUp() {
        dbPlatform = DbPlatform.fromDataSource(dataSource);
        jdbcTemplate = new NamedParameterJdbcTemplate(getDataSource());
    }

    @Override
    public RdfStream getInboundReferences(@Nonnull final Transaction tx, final FedoraResource resource) {
        final String resourceId = resource.getFedoraId().getFullId();
        final Node subject = NodeFactory.createURI(resourceId);
        final Stream<Triple> stream = getReferencesInternal(tx, resourceId);
        if (resource instanceof NonRdfSourceDescription) {
            final Stream<Triple> stream2 = getReferencesInternal(tx, resource.getFedoraId().getBaseId());
            return new DefaultRdfStream(subject, Stream.concat(stream, stream2));
        }
        return new DefaultRdfStream(subject, stream);
    }

    /**
     * Get the inbound references for the resource Id and the transaction id.
     * @param tx transaction or null for none.
     * @param targetId the id that will be the target of references.
     * @return RDF stream of inbound references
     */
    private Stream<Triple> getReferencesInternal(final Transaction tx, final String targetId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("targetId", targetId);
        final Node targetNode = NodeFactory.createURI(targetId);

        final RowMapper<Triple> inboundMapper = (rs, rowNum) ->
                Triple.create(NodeFactory.createURI(rs.getString(SUBJECT_COLUMN)),
                        NodeFactory.createURI(rs.getString(PROPERTY_COLUMN)),
                        targetNode);

        final String query;

        if (tx.isOpenLongRunning()) {
            // we are in a transaction
            parameterSource.addValue("transactionId", tx.getId());
            query = SELECT_INBOUND_IN_TRANSACTION;
        } else {
            // not in a transaction
            query = SELECT_INBOUND;
        }

        final var references = jdbcTemplate.query(query, parameterSource, inboundMapper);

        LOGGER.debug("getInboundReferences for {} in transaction {} found {} references",
                targetId, tx, references.size());
        return references.stream();
    }

    @Override
    public void deleteAllReferences(@Nonnull final Transaction tx, final FedoraId resourceId) {
        final List<Quad> deleteReferences = getOutboundReferences(tx, resourceId);
        if (resourceId.isDescription()) {
            // Also get the binary references
            deleteReferences.addAll(getOutboundReferences(tx, resourceId.asBaseId()));
        }
        // Remove all the existing references.
        deleteReferences.forEach(t -> removeReference(tx, t));
    }

    /**
     * Get a stream of quads of resources being referenced from the provided resource, the graph of the quad is the
     * URI of the resource the reference is from.
     * @param tx transaction Id or null if none.
     * @param resourceId the resource Id.
     * @return list of Quads
     */
    private List<Quad> getOutboundReferences(final Transaction tx, final FedoraId resourceId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("resourceId", resourceId.getFullId());
        final Node subjectNode = NodeFactory.createURI(resourceId.getFullId());

        final RowMapper<Quad> outboundMapper = (rs, rowNum) ->
                Quad.create(subjectNode,
                        NodeFactory.createURI(rs.getString(SUBJECT_COLUMN)),
                        NodeFactory.createURI(rs.getString(PROPERTY_COLUMN)),
                        NodeFactory.createURI(rs.getString(TARGET_COLUMN)));

        final String query;

        if (tx.isOpenLongRunning()) {
            // we are in a long-running transaction
            parameterSource.addValue("transactionId", tx.getId());
            query = SELECT_OUTBOUND_IN_TRANSACTION;
        } else {
            // not in a transaction or in a short-lived transaction
            query = SELECT_OUTBOUND;
        }

        final var references = jdbcTemplate.query(query, parameterSource, outboundMapper);

        LOGGER.debug("getOutboundReferences for {} in transaction {} found {} references",
                resourceId, tx, references.size());
        return references;
    }

    @Override
    public void updateReferences(@Nonnull final Transaction tx, final FedoraId resourceId, final String userPrincipal,
                                 final RdfStream rdfStream) {
        try {
            final List<Triple> addReferences = getReferencesFromRdf(rdfStream).toList();
            var referencesStream = addReferences.stream();

            final Node resourceNode = NodeFactory.createURI(resourceId.getFullId());
            // Only need to check existing references if initialization is complete, indicating we are not reindexing
            if (initializationStatus.isInitializationComplete()) {
                // This predicate checks for items we are adding, so we don't bother to delete and then re-add them.
                final Predicate<Quad> notInAdds = q -> !addReferences.contains(q.asTriple());
                // References from this resource.
                final List<Quad> existingReferences = getOutboundReferences(tx, resourceId);
                if (resourceId.isDescription()) {
                    // Resource is a binary description so also get the binary references.
                    existingReferences.addAll(getOutboundReferences(tx, resourceId.asBaseId()));
                }
                // Remove any existing references not being re-added.
                existingReferences.stream().filter(notInAdds).forEach(t -> removeReference(tx, t));

                // This predicate checks for references that didn't already exist in the database.
                final Predicate<Triple> alreadyExists =
                        t -> !existingReferences.contains(Quad.create(resourceNode, t));
                referencesStream = referencesStream.filter(alreadyExists);
            }
            // Add the new references.
            referencesStream.forEach(r ->
                    addReference(tx, Quad.create(resourceNode, r), userPrincipal));
        } catch (final Exception e) {
            LOGGER.warn("Unable to update reference index for resource {} in transaction {}: {}",
                    resourceId.getFullId(), tx.getId(), e.getMessage());
            throw new RepositoryRuntimeException("Unable to update reference index", e);
        }
    }

    @Override
    public void commitTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            tx.ensureCommitting();
            try {
                final Map<String, String> parameterSource = Map.of("transactionId", tx.getId());
                jdbcTemplate.update(COMMIT_DELETE_RECORD_MAP.get(dbPlatform), parameterSource);
                jdbcTemplate.update(COMMIT_ADD_RECORDS, parameterSource);
                jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);
            } catch (final Exception e) {
                LOGGER.warn("Unable to commit reference index transaction {}: {}", tx, e.getMessage());
                throw new RepositoryRuntimeException("Unable to commit reference index transaction", e);
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void rollbackTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            try {
                final Map<String, String> parameterSource = Map.of("transactionId", tx.getId());
                jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);
            } catch (final Exception e) {
                LOGGER.warn("Unable to rollback reference index transaction {}: {}", tx, e.getMessage());
                throw new RepositoryRuntimeException("Unable to rollback reference index transaction", e);
            }
        }
    }

    @Override
    public void clearAllTransactions() {
        jdbcTemplate.update(TRUNCATE_TX_TABLE, Map.of());
    }

    @Override
    public void reset() {
        try {
            jdbcTemplate.update(TRUNCATE_TABLE, Map.of());
            jdbcTemplate.update(TRUNCATE_TX_TABLE, Map.of());
        } catch (final Exception e) {
            LOGGER.warn("Unable to reset reference index: {}", e.getMessage());
            throw new RepositoryRuntimeException("Unable to reset reference index", e);
        }
    }

    /**
     * Remove a reference.
     * @param tx the transaction
     * @param reference the quad with the reference, is Quad(resourceId, subjectId, propertyId, targetId)
     */
    private void removeReference(final Transaction tx, final Quad reference) {
        tx.doInTx(() -> {
            final var parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("resourceId", reference.getGraph().getURI());
            parameterSource.addValue("subjectId", reference.getSubject().getURI());
            parameterSource.addValue("property", reference.getPredicate().getURI());
            parameterSource.addValue("targetId", reference.getObject().getURI());

            if (!tx.isShortLived()) {
                parameterSource.addValue("transactionId", tx.getId());
                final boolean addedInTx = !jdbcTemplate.queryForList(IS_REFERENCE_ADDED_IN_TRANSACTION, parameterSource)
                        .isEmpty();
                if (addedInTx) {
                    jdbcTemplate.update(UNDO_INSERT_REFERENCE_IN_TRANSACTION, parameterSource);
                } else {
                    jdbcTemplate.update(DELETE_REFERENCE_IN_TRANSACTION, parameterSource);
                }
            } else {
                jdbcTemplate.update(DELETE_REFERENCE_DIRECT, parameterSource);
            }
        });
    }

    /**
     * Add a reference
     * @param transaction the transaction Id.
     * @param reference the quad with the reference, is is Quad(resourceId, subjectId, propertyId, targetId)
     * @param userPrincipal the user adding the reference.
     */
    private void addReference(@Nonnull final Transaction transaction, final Quad reference,
                              final String userPrincipal) {
        transaction.doInTx(() -> {
            final String targetId = reference.getObject().getURI();

            final var parameterSource = new MapSqlParameterSource();
            parameterSource.addValue("resourceId", reference.getGraph().getURI());
            parameterSource.addValue("subjectId", reference.getSubject().getURI());
            parameterSource.addValue("property", reference.getPredicate().getURI());
            parameterSource.addValue("targetId", targetId);

            if (!transaction.isShortLived()) {
                parameterSource.addValue("transactionId", transaction.getId());
                final boolean addedInTx = !jdbcTemplate.queryForList(
                        IS_REFERENCE_DELETED_IN_TRANSACTION, parameterSource)
                        .isEmpty();
                if (addedInTx) {
                    jdbcTemplate.update(UNDO_DELETE_REFERENCE_IN_TRANSACTION, parameterSource);
                } else {
                    jdbcTemplate.update(INSERT_REFERENCE_IN_TRANSACTION, parameterSource);
                    recordEvent(transaction, targetId, userPrincipal);
                }
            } else {
                jdbcTemplate.update(INSERT_REFERENCE_DIRECT, parameterSource);
                recordEvent(transaction, targetId, userPrincipal);
            }
        });
    }

    /**
     * Record the inbound reference event if the target exists.
     * @param transaction the transaction.
     * @param resourceId the id of the target of the inbound reference.
     * @param userPrincipal the user making the reference.
     */
    private void recordEvent(final Transaction transaction, final String resourceId, final String userPrincipal) {
        final FedoraId fedoraId = FedoraId.create(resourceId);
        if (this.containmentIndex.resourceExists(transaction, fedoraId, false)) {
            this.eventAccumulator.recordEventForOperation(transaction, fedoraId, getOperation(transaction, fedoraId,
                    userPrincipal));
        }
    }

    /**
     * Create a ReferenceOperation for the current add.
     * @param tx the transaction for the current operation.
     * @param id the target resource of the reference.
     * @param user the user making the change
     * @return a ReferenceOperation
     */
    private static ReferenceOperation getOperation(final Transaction tx, final FedoraId id, final String user) {
        final ReferenceOperationBuilder builder = new ReferenceOperationBuilder(tx, id);
        builder.userPrincipal(user);
        return builder.build();
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
        return stream.filter(isInternalReference);
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
