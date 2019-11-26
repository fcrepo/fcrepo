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
package org.fcrepo.persistence.ocfl;

import static java.lang.String.format;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.resolveVersionId;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentSessionClosedException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.api.Persister;
import org.fcrepo.persistence.ocfl.impl.DeleteResourcePersister;
import org.fcrepo.persistence.ocfl.impl.FedoraOCFLMapping;
import org.fcrepo.persistence.ocfl.impl.NonRdfSourcePersister;
import org.fcrepo.persistence.ocfl.impl.RDFSourcePersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getRDFFileExtension;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getRdfStream;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.relativizeSubpath;

import static org.fcrepo.persistence.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.RESOURCE_HEADER_EXTENSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.resolveOCFLSubpath;

/**
 * OCFL Persistent Storage class.
 *
 * @author whikloj
 * @since 2019-09-20
 */
public class OCFLPersistentStorageSession implements PersistentStorageSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(OCFLPersistentStorageSession.class);

    /**
     * Externally generated id for the session.
     */
    private final String sessionId;

    private final FedoraToOCFLObjectIndex fedoraOcflIndex;

    private final Map<String, OCFLObjectSession> sessionMap;

    private final Phaser phaser = new Phaser();

    private final static List<Persister> PERSISTER_LIST = new ArrayList<>();

    private List<CommittedSession> sessionsToRollback = new ArrayList<>();

    private State state = State.COMMIT_NOT_STARTED;

    static {
        PERSISTER_LIST.add(new RDFSourcePersister());
        PERSISTER_LIST.add(new NonRdfSourcePersister());
        PERSISTER_LIST.add(new DeleteResourcePersister());
        //TODO add new persisters here as they are implemented.
    }

    private OCFLObjectSessionFactory objectSessionFactory;

    private class CommittedSession {
        final OCFLObjectSession session;
        final CommitOption option;

        CommittedSession(final OCFLObjectSession session, final CommitOption option) {
            this.session = session;
            this.option = option;
        }
    }

    private enum State {
        COMMIT_NOT_STARTED,
        COMMIT_STARTED,
        PREPARE_FAILED,
        COMMITTED,
        COMMIT_FAILED,
        ROLLING_BACK,
        ROLLED_BACK,
        ROLLBACK_FAILED;
    }

    /**
     * Constructor
     *
     * @param sessionId            session id.
     * @param fedoraOcflIndex      the index
     * @param objectSessionFactory the session factory
     */
    protected OCFLPersistentStorageSession(final String sessionId, final FedoraToOCFLObjectIndex fedoraOcflIndex,
                                           final OCFLObjectSessionFactory objectSessionFactory) {
        this.sessionId = sessionId;
        this.fedoraOcflIndex = fedoraOcflIndex;
        this.objectSessionFactory = objectSessionFactory;
        this.sessionMap = new ConcurrentHashMap<>();
    }

    /**
     * Constructor
     *
     * @param fedoraOcflIndex      the index
     * @param objectSessionFactory the session factory
     */
    protected OCFLPersistentStorageSession(final FedoraToOCFLObjectIndex fedoraOcflIndex,
                                           final OCFLObjectSessionFactory objectSessionFactory) {
        this(null, fedoraOcflIndex, objectSessionFactory);
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public void persist(final ResourceOperation operation) throws PersistentStorageException {
        actionNeedsWrite();
        ensureCommitNotStarted();

        try {
            phaser.register();

            //resolve the persister based on the operation
            final var persister = PERSISTER_LIST.stream().filter(p -> p.handle(operation)).findFirst().orElse(null);

            if (persister == null) {
                throw new UnsupportedOperationException(format("The %s is not yet supported", operation.getClass()));
            }

            //resolve the mapping between the fedora resource identifier and the associated OCFL object.
            final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(operation.getResourceId());

            //get the session.
            final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());

            //perform the operation
            persister.persist(this, objSession, operation, mapping);

        } finally {
            phaser.arriveAndDeregister();
        }

    }

    private void ensureCommitNotStarted() throws PersistentSessionClosedException {
        if (!state.equals(State.COMMIT_NOT_STARTED)) {
            throw new PersistentSessionClosedException("The session cannot be committed in the  " + state + " state");
        }
    }


    private OCFLObjectSession findOrCreateSession(final String ocflId) {
        final OCFLObjectSession sessionObj = this.sessionMap.get(ocflId);
        if (sessionObj != null) {
            return sessionObj;
        }

        final OCFLObjectSession newSession = this.objectSessionFactory.create(ocflId, getId());
        sessionMap.put(ocflId, newSession);
        return newSession;
    }

    @Override
    public ResourceHeaders getHeaders(final String identifier, final Instant version)
            throws PersistentStorageException {

        ensureCommitNotStarted();

        final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(identifier);
        final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());
        final String fedoraSubpath = relativizeSubpath(mapping.getParentFedoraResourceId(), identifier);
        final String ocflSubpath = resolveOCFLSubpath(fedoraSubpath);
        final String sidecarSubpath = getInternalFedoraDirectory() + ocflSubpath + RESOURCE_HEADER_EXTENSION;

        final InputStream headerStream;
        if (version != null) {
            final String versionId = resolveVersionId(objSession, version);
            headerStream = objSession.read(sidecarSubpath, versionId);
        } else {
            headerStream = objSession.read(sidecarSubpath);
        }

        return deserializeHeaders(headerStream);
    }

    @Override
    public RdfStream getTriples(final String identifier, final Instant version)
            throws PersistentStorageException {
        ensureCommitNotStarted();

        final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(identifier);
        final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());
        final String fedoraSubpath = relativizeSubpath(mapping.getParentFedoraResourceId(), identifier);
        final String ocflSubpath = resolveOCFLSubpath(fedoraSubpath);
        final String filePath = ocflSubpath + getRDFFileExtension();
        return getRdfStream(identifier, objSession, filePath, version);
    }

    /**
     * Returns a list of immutable versions associated with the specified fedora identifier
     * @param fedoraIdentifier The fedora identifier
     * @return The list of instants that map to the underlying versions
     * @throws PersistentStorageException Due the underlying resource not existing or is otherwise unreadable.
     */
     List<Instant> listVersions(final String fedoraIdentifier) throws PersistentStorageException {
        final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(fedoraIdentifier);
        final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());
        return OCFLPersistentStorageUtils.listVersions(objSession);
    }

    @Override
    public RdfStream getManagedProperties(final String identifier, final Instant version)
            throws PersistentStorageException {
        ensureCommitNotStarted();
        final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(identifier);
        final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());
        final String fedoraSubpath = relativizeSubpath(mapping.getParentFedoraResourceId(), identifier);
        final String ocflSubpath = resolveOCFLSubpath(fedoraSubpath);
        final String filePath = getInternalFedoraDirectory() + ocflSubpath + getRDFFileExtension();
        return getRdfStream(identifier, objSession, filePath, version);
    }

    @Override
    public InputStream getBinaryContent(final String identifier, final Instant version) throws PersistentItemNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized void commit() throws PersistentStorageException {
        ensureCommitNotStarted();
        if (isReadOnly()) {
            // No changes to commit.
            return;
        }

        this.state = State.COMMIT_STARTED;
        LOGGER.debug("Starting commit.");

        phaser.arriveAndAwaitAdvance();

        LOGGER.debug("All persisters are complete");

        //prepare session for commit
        final Collection<OCFLObjectSession> sessions = this.sessionMap.values();

        try {
            LOGGER.debug("Preparing commit...");

            for (final OCFLObjectSession objectSession : sessions) {
                objectSession.prepare();
            }

            LOGGER.debug("Prepare succeeded.");
        } catch (final Exception e) {
            this.state = State.PREPARE_FAILED;
            throw new PersistentStorageException("Commit failed due to : " + e.getMessage(), e);
        }

        try {
            this.sessionsToRollback = new ArrayList<>(sessions.size());

            //perform commit
            for (final OCFLObjectSession objectSession : sessions) {
                final CommitOption option = objectSession.getDefaultCommitOption();
                objectSession.commit(option);
                sessionsToRollback.add(new CommittedSession(objectSession, option));
            }

            state = State.COMMITTED;
            LOGGER.info("Successfully committed {}", this);
        } catch (final Exception e) {
            this.state = State.COMMIT_FAILED;
            throw new PersistentStorageException("Commit failed due to : " + e.getMessage(), e);
        }
    }

    @Override
    public void rollback() throws PersistentStorageException {
        if (isReadOnly()) {
            // No changes to rollback
            return;
        }

        if (!(state.equals(State.COMMIT_FAILED) || state.equals(State.PREPARE_FAILED) ||
                state.equals(State.COMMIT_NOT_STARTED))) {
            throw new PersistentStorageException("This session cannot be rolled back in this state: " + state);
        }

        final boolean commitWasStarted = this.state != State.COMMIT_NOT_STARTED;

        this.state = State.ROLLING_BACK;
        LOGGER.info("rolling back...");

        if (!commitWasStarted) {
            //if the commit had not been started at the time this method was invoked
            //we must ensure that all persist operations are complete before we close any
            //ocfl object sessions. If the commit had been started then this synchronization step
            //will have already occurred and is thus unnecessary.
            this.phaser.arriveAndAwaitAdvance();
        }

        //close any uncommitted sessions
        final List<OCFLObjectSession> committedSessions = this.sessionsToRollback.stream().map(c -> c.session).collect(Collectors.toList());
        final List<OCFLObjectSession> uncommittedSessions = new ArrayList<>(this.sessionMap.values());
        uncommittedSessions.removeAll(committedSessions);
        for (final OCFLObjectSession obj : uncommittedSessions) {
            obj.close();
        }

        if (commitWasStarted) {

            // rollback committed sessions
            //for each committed session, rollback if possible
            final List<String> rollbackFailures = new ArrayList<>(this.sessionsToRollback.size());
            for (final CommittedSession cs : this.sessionsToRollback) {
                if (cs.option == NEW_VERSION) {
                    //TODO rollback to previous OCFL version
                    //add any failure messages here.
                    rollbackFailures.add(format("rollback of previously committed versions is not yet supported",
                            cs.session));
                } else {
                    rollbackFailures.add(format("%s was already committed to the unversioned head", cs.session));
                }
            }
            //throw an exception if any sessions could not be rolled back.
            if (rollbackFailures.size() > 0) {
                state = State.ROLLBACK_FAILED;
                final StringBuilder builder = new StringBuilder();
                builder.append("Unable to rollback successfully due to the following reasons: \n");
                for (final String failures : rollbackFailures) {
                    builder.append("        " + failures + "\n");
                }

                throw new PersistentStorageException(builder.toString());
            }
        }
        this.state = State.ROLLED_BACK;
        LOGGER.info("rolled back successfully.");

    }

    /**
     * Check if we are in a read-only session.
     *
     * @return whether we are read-only (ie. no transaction).
     */
    private boolean isReadOnly() {
        return this.sessionId == null;
    }

    /**
     * Utility to throw exception if trying to perform write operation on read-only session.
     */
    private void actionNeedsWrite() throws PersistentStorageException {
        if (isReadOnly()) {
            throw new PersistentStorageException("Session is read-only");
        }
    }
}
