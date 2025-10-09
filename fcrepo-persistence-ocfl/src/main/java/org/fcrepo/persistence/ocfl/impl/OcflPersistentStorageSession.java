/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentSessionClosedException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.Persister;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflVersionInfo;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.api.agent.Trace;

/**
 * OCFL Persistent Storage class.
 *
 * @author whikloj
 * @since 2019-09-20
 */
public class OcflPersistentStorageSession implements PersistentStorageSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcflPersistentStorageSession.class);

    private static final long AWAIT_TIMEOUT = 30000L;

    /**
     * Externally generated Transaction for the session.
     */
    private final Transaction transaction;

    private final FedoraToOcflObjectIndex fedoraOcflIndex;

    private final Map<String, OcflObjectSession> sessionMap;

    private final ReindexService reindexSerivce;

    private Map<String, OcflObjectSession> sessionsToRollback;

    private final Phaser phaser = new Phaser();

    private final List<Persister> persisterList = new ArrayList<>();

    private State state = State.COMMIT_NOT_STARTED;

    private final OcflObjectSessionFactory objectSessionFactory;

    private enum State {
        COMMIT_NOT_STARTED(true),
        PREPARE_STARTED(false),
        PREPARED(true),
        PREPARE_FAILED(true),
        COMMIT_STARTED(false),
        COMMITTED(true),
        COMMIT_FAILED(true),
        ROLLING_BACK(false),
        ROLLED_BACK(false),
        ROLLBACK_FAILED(false);

        final boolean rollbackAllowed;

        State(final boolean rollbackAllowed) {
            this.rollbackAllowed = rollbackAllowed;
        }

    }

    /**
     * Constructor
     *
     * @param tx                   the transaction.
     * @param fedoraOcflIndex      the index
     * @param objectSessionFactory the session factory
     */
    protected OcflPersistentStorageSession(final Transaction tx,
                                           final FedoraToOcflObjectIndex fedoraOcflIndex,
                                           final OcflObjectSessionFactory objectSessionFactory,
                                           final ReindexService reindexService) {
        this.transaction = tx;
        this.fedoraOcflIndex = fedoraOcflIndex;
        this.objectSessionFactory = objectSessionFactory;
        this.reindexSerivce = reindexService;
        this.sessionsToRollback = new HashMap<>();

        if (!tx.isReadOnly()) {
            this.sessionMap = new ConcurrentHashMap<>();
        } else {
            // The read-only session is never closed, so it needs to periodically expire object sessions
            this.sessionMap = Caffeine.newBuilder()
                    .maximumSize(512)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .<String, OcflObjectSession>build()
                    .asMap();
        }

        //load the persister list if empty
        persisterList.add(new CreateRdfSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new OverwriteRdfTombstonePersister(this.fedoraOcflIndex));
        persisterList.add(new UpdateRdfSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new UpdateNonRdfSourceHeadersPersister(this.fedoraOcflIndex));
        persisterList.add(new CreateNonRdfSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new UpdateNonRdfSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new DeleteResourcePersister(this.fedoraOcflIndex));
        persisterList.add(new CreateVersionPersister(this.fedoraOcflIndex));
        persisterList.add(new PurgeResourcePersister(this.fedoraOcflIndex));
        persisterList.add(new ReindexResourcePersister(this.reindexSerivce));

    }

    @Override
    public String getId() {
        return this.transaction.getId();
    }

    @Override
    public void persist(final ResourceOperation operation) throws PersistentStorageException {
        actionNeedsWrite();
        ensureCommitNotStarted();

        try {
            phaser.register();

            //resolve the persister based on the operation
            final var persister = persisterList.stream().filter(p -> p.handle(operation)).findFirst().orElse(null);

            if (persister == null) {
                throw new UnsupportedOperationException(format("The %s is not yet supported", operation.getClass()));
            }

            //perform the operation
            persister.persist(this, operation);

        } finally {
            phaser.arriveAndDeregister();
        }

    }

    private void ensureCommitNotStarted() throws PersistentSessionClosedException {
        if (!state.equals(State.COMMIT_NOT_STARTED)) {
            throw new PersistentSessionClosedException(
                    String.format("Storage session %s is already closed", transaction));
        }
    }

    private void ensurePrepared() throws PersistentSessionClosedException {
        if (!state.equals(State.PREPARED)) {
            throw new PersistentStorageException(
                    String.format("Storage session %s cannot be committed because it is not in the correct state: %s",
                            transaction, state));
        }
    }

    @Trace
    OcflObjectSession findOrCreateSession(final String ocflId) {
        return this.sessionMap.computeIfAbsent(ocflId, key -> {
            return new FcrepoOcflObjectSessionWrapper(this.objectSessionFactory.newSession(key));
        });
    }

    @Trace
    @Override
    public ResourceHeaders getHeaders(final FedoraId identifier, final Instant version)
            throws PersistentStorageException {
        ensureCommitNotStarted();

        final FedoraOcflMapping mapping = getFedoraOcflMapping(identifier);
        final OcflObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());

        final var versionId = resolveVersionNumber(objSession, identifier, version);
        final var headers = objSession.readHeaders(identifier.getResourceId(), versionId);

        return new ResourceHeadersAdapter(headers).asKernelHeaders();
    }

    @Trace
    private FedoraOcflMapping getFedoraOcflMapping(final FedoraId identifier)
            throws PersistentStorageException {
        try {
            return fedoraOcflIndex.getMapping(transaction, identifier);
        } catch (final FedoraOcflMappingNotFoundException e) {
            throw new PersistentItemNotFoundException(String.format("Resource %s not found",
                    identifier.getFullIdPath()), e);
        }
    }

    @Trace
    @Override
    public RdfStream getTriples(final FedoraId identifier, final Instant version)
            throws PersistentStorageException {
        ensureCommitNotStarted();

        LOGGER.debug("Getting triples for {} at {}", identifier, version);

        try (final InputStream is = getBinaryContent(identifier, version)) {
            final Model model = createDefaultModel();
            RDFDataMgr.read(model, is, OcflPersistentStorageUtils.getRdfFormat().getLang());
            final FedoraId topic = resolveTopic(identifier);
            return DefaultRdfStream.fromModel(createURI(topic.getFullId()), model);
        } catch (final IOException ex) {
            throw new PersistentStorageException(format("unable to read %s ;  version = %s", identifier, version), ex);
        }
    }

    @Trace
    @Override
    public List<Instant> listVersions(final FedoraId fedoraIdentifier)
            throws PersistentStorageException {
        final var mapping = getFedoraOcflMapping(fedoraIdentifier);
        final var objSession = findOrCreateSession(mapping.getOcflObjectId());

        return objSession.listVersions(fedoraIdentifier.getResourceId()).stream()
                .map(OcflVersionInfo::getCreated)
                .collect(Collectors.toList());
    }

    @Override
    public InputStream getBinaryContent(final FedoraId identifier, final Instant version)
            throws PersistentStorageException {
        ensureCommitNotStarted();

        final var mapping = getFedoraOcflMapping(identifier);
        final var objSession = findOrCreateSession(mapping.getOcflObjectId());

        final var versionNumber = resolveVersionNumber(objSession, identifier, version);

        return objSession.readContent(identifier.getResourceId(), versionNumber)
                .getContentStream()
                .orElseThrow(() -> new PersistentItemNotFoundException("No binary content found for resource "
                        + identifier.getFullId()));
    }

    @Override
    public InputStream getBinaryRange(final FedoraId identifier, final Instant version,
                                      final long start, final long end) throws PersistentStorageException {
        ensureCommitNotStarted();

        final var mapping = getFedoraOcflMapping(identifier);
        final var objSession = findOrCreateSession(mapping.getOcflObjectId());

        final var versionNumber = resolveVersionNumber(objSession, identifier, version);

        return objSession.readRange(identifier.getResourceId(), versionNumber, start, end)
                .getContentStream()
                .orElseThrow(() -> new PersistentItemNotFoundException("No binary content found for resource "
                        + identifier.getFullId()));
    }

    @Override
    public synchronized void prepare() {
        ensureCommitNotStarted();
        if (isReadOnly()) {
            // No changes to commit.
            return;
        }

        this.state = State.PREPARE_STARTED;
        LOGGER.debug("Starting storage session {} prepare for commit", transaction);

        if (this.phaser.getRegisteredParties() > 0) {
            this.phaser.awaitAdvance(0);
        }

        LOGGER.trace("All persisters are complete in session {}", transaction);

        try {
            fedoraOcflIndex.commit(transaction);
            state = State.PREPARED;
        } catch (final RuntimeException e) {
            state = State.PREPARE_FAILED;
            throw new PersistentStorageException(String.format("Failed to prepare storage session <%s> for commit",
                    transaction), e);
        }
    }

    @Override
    public synchronized void commit() throws PersistentStorageException {
        ensurePrepared();
        if (isReadOnly()) {
            // No changes to commit.
            return;
        }

        this.state = State.COMMIT_STARTED;
        LOGGER.debug("Starting storage session {} commit", transaction);

        // order map for testing
        final var sessions = new TreeMap<>(sessionMap);
        commitObjectSessions(sessions);

        LOGGER.debug("Committed storage session {}", transaction);
    }

    private void commitObjectSessions(final Map<String, OcflObjectSession> sessions)
            throws PersistentStorageException {
        this.sessionsToRollback = new HashMap<>(sessionMap.size());

        for (final var entry : sessions.entrySet()) {
            final var id = entry.getKey();
            final var session = entry.getValue();
            try {
                session.commit();
                sessionsToRollback.put(id, session);
            } catch (final Exception e) {
                this.state = State.COMMIT_FAILED;
                throw new PersistentStorageException(String.format("Failed to commit object <%s> in session <%s>",
                        id, transaction), e);
            }
        }

        state = State.COMMITTED;
    }

    @Override
    public void rollback() throws PersistentStorageException {
        if (isReadOnly()) {
            // No changes to rollback
            return;
        }

        if (!state.rollbackAllowed) {
            throw new PersistentStorageException("This session cannot be rolled back in this state: " + state);
        }

        final boolean commitWasStarted = this.state != State.COMMIT_NOT_STARTED;

        this.state = State.ROLLING_BACK;
        LOGGER.debug("Rolling back storage session {}", transaction);

        if (!commitWasStarted) {
            //if the commit had not been started at the time this method was invoked
            //we must ensure that all persist operations are complete before we close any
            //ocfl object sessions. If the commit had been started then this synchronization step
            //will have already occurred and is thus unnecessary.
            if (this.phaser.getRegisteredParties() > 0) {
                try {
                    this.phaser.awaitAdvanceInterruptibly(0, AWAIT_TIMEOUT, MILLISECONDS);
                } catch (final InterruptedException | TimeoutException e) {
                    throw new PersistentStorageException(
                            "Waiting for operations to complete took too long, rollback failed");
                }
            }
        }

        try {
            closeUncommittedSessions();

            if (commitWasStarted) {
                rollbackCommittedSessions();
            }
        } finally {
            // Always roll back the index changes associated with the transaction
            try {
                fedoraOcflIndex.rollback(transaction);
            } catch (final Exception e) {
                LOGGER.error("Failed to rollback OCFL index updates in transaction {}", transaction, e);
            }
        }

        this.state = State.ROLLED_BACK;
        LOGGER.trace("Successfully rolled back storage session {}", transaction);
    }

    /**
     * Resolve an instant to a version
     *
     * @param objSession session
     * @param fedoraId the FedoraId of the resource
     * @param version version time
     * @return name of version
     * @throws PersistentStorageException thrown if version not found
     */
    @Trace
    private String resolveVersionNumber(final OcflObjectSession objSession,
                                       final FedoraId fedoraId,
                                       final Instant version)
            throws PersistentStorageException {
        if (version != null) {
            final var versions = objSession.listVersions(fedoraId.getResourceId());
            // reverse order so that the most recent version is matched first
            Collections.reverse(versions);
            return versions.stream()
                    .filter(vd -> vd.getCreated().equals(version))
                    .map(OcflVersionInfo::getVersionNumber)
                    .findFirst()
                    .orElseThrow(() -> {
                        return new PersistentItemNotFoundException(format(
                                "There is no version in %s with a created date matching %s",
                                fedoraId, version));
                    });
        }

        return null;
    }

    private void closeUncommittedSessions() {
        this.sessionMap.entrySet().stream()
                .filter(entry -> !sessionsToRollback.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .forEach(OcflObjectSession::abort);
    }

    private void rollbackCommittedSessions() throws PersistentStorageException {
        final List<String> rollbackFailures = new ArrayList<>(this.sessionsToRollback.size());

        for (final var entry : this.sessionsToRollback.entrySet()) {
            final var id = entry.getKey();
            final var session = entry.getValue();

            try {
                session.rollback();
            } catch (final Exception e) {
                rollbackFailures.add(String.format("Failed to rollback object <%s> in session <%s>: %s",
                        id, session.sessionId(), e.getMessage()));
            }
        }

        //throw an exception if any sessions could not be rolled back.
        if (rollbackFailures.size() > 0) {
            state = State.ROLLBACK_FAILED;
            final StringBuilder builder = new StringBuilder()
                    .append("Unable to rollback storage session ")
                    .append(transaction)
                    .append(" completely due to the following errors: \n");

            for (final String failures : rollbackFailures) {
                builder.append("\t").append(failures).append("\n");
            }

            throw new PersistentStorageException(builder.toString());
        }
    }

    /**
     * Check if we are in a read-only session.
     *
     * @return whether we are read-only (ie. no transaction).
     */
    private boolean isReadOnly() {
        return this.transaction.isReadOnly();
    }

    /**
     * Utility to throw exception if trying to perform write operation on read-only session.
     */
    private void actionNeedsWrite() throws PersistentStorageException {
        if (isReadOnly()) {
            throw new PersistentStorageException("Session is read-only");
        }
    }

    /**
     * Returns the RDF topic to be returned for a given resource identifier
     * For example:  passing info:fedora/resource1/fcr:metadata would return
     *  info:fedora/resource1 since  info:fedora/resource1 would be the expected
     *  topic.
     * @param fedoraIdentifier The fedora identifier
     * @return The resolved topic
     */
    private FedoraId resolveTopic(final FedoraId fedoraIdentifier) {
        if (fedoraIdentifier.isDescription()) {
            return fedoraIdentifier.asBaseId();
        } else {
            return fedoraIdentifier;
        }
    }

    @Override
    public String toString() {
        return "OcflPersistentStorageSession{" +
                "sessionId='" + transaction + '\'' +
                ", state=" + state +
                '}';
    }

}
