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
package org.fcrepo.persistence.ocfl.impl;

import static java.lang.String.format;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getSidecarSubpath;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.resolveVersionId;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getBinaryStream;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.api.Persister;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.resovleOCFLSubpathFromResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getRDFFileExtension;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getRdfStream;

import static org.fcrepo.persistence.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;

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

    private final List<Persister> persisterList = new ArrayList<>();

    private List<CommittedSession> sessionsToRollback = new ArrayList<>();

    private State state = State.COMMIT_NOT_STARTED;

    private OCFLObjectSessionFactory objectSessionFactory;

    private static Comparator<OCFLObjectSession> CREATION_TIME_ORDER =
            (OCFLObjectSession o1, OCFLObjectSession o2)->o1.getCreated().compareTo(o2.getCreated());


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

        //load the persister list if empty
        persisterList.add(new CreateRDFSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new UpdateRDFSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new CreateNonRdfSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new UpdateNonRdfSourcePersister(this.fedoraOcflIndex));
        persisterList.add(new DeleteResourcePersister(this.fedoraOcflIndex));

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
            throw new PersistentSessionClosedException("The session cannot be committed in the  " + state + " state");
        }
    }


    OCFLObjectSession findOrCreateSession(final String ocflId) {
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

        final FedoraOCFLMapping mapping = getFedoraOCFLMapping(identifier);
        final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());
        final var rootIdentifier = mapping.getRootObjectIdentifier();
        final var ocflSubpath = resovleOCFLSubpathFromResourceId(rootIdentifier, identifier);
        final var sidecarSubpath = getSidecarSubpath(ocflSubpath);

        final InputStream headerStream;
        if (version != null) {
            final var versionId = resolveVersionId(objSession, version);
            headerStream = objSession.read(sidecarSubpath, versionId);
        } else {
            headerStream = objSession.read(sidecarSubpath);
        }

        return deserializeHeaders(headerStream);
    }

    private FedoraOCFLMapping getFedoraOCFLMapping(final String identifier) throws PersistentStorageException {
        try {
            return fedoraOcflIndex.getMapping(identifier);
        } catch (FedoraOCFLMappingNotFoundException e) {
            throw new PersistentItemNotFoundException(e.getMessage());
        }
    }

    @Override
    public RdfStream getTriples(final String identifier, final Instant version)
            throws PersistentStorageException {
        ensureCommitNotStarted();

        final var mapping = getFedoraOCFLMapping(identifier);
        final var rootIdentifier = mapping.getRootObjectIdentifier();
        final var objSession = findOrCreateSession(mapping.getOcflObjectId());
        final var ocflSubpath = resovleOCFLSubpathFromResourceId(rootIdentifier, identifier);
        final var filePath = ocflSubpath + getRDFFileExtension();
        return getRdfStream(identifier, objSession, filePath, version);
    }

    @Override
    public List<Instant> listVersions(final String fedoraIdentifier) throws PersistentStorageException {
        final var mapping = getFedoraOCFLMapping(fedoraIdentifier);
        final var objSession = findOrCreateSession(mapping.getOcflObjectId());

        String subpath = null;

        // Find the subpath if it's a child of an AG
        if (!mapping.getRootObjectIdentifier().equals(fedoraIdentifier)) {
            subpath = resovleOCFLSubpathFromResourceId(mapping.getRootObjectIdentifier(), fedoraIdentifier);

            final var headers = getHeaders(fedoraIdentifier, null);
            if (!NON_RDF_SOURCE.getURI().equals(headers.getInteractionModel())) {
                subpath += getRDFFileExtension();
            }
        }

        return OCFLPersistentStorageUtils.listVersions(objSession, subpath);
    }

    @Override
    public InputStream getBinaryContent(final String identifier, final Instant version)
            throws PersistentStorageException {
        ensureCommitNotStarted();

        final var mapping = getFedoraOCFLMapping(identifier);
        final var rootIdentifier = mapping.getRootObjectIdentifier();
        final var objSession = findOrCreateSession(mapping.getOcflObjectId());
        final var ocflSubpath = resovleOCFLSubpathFromResourceId(rootIdentifier, identifier);

        return getBinaryStream(objSession, ocflSubpath, version);
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
        final List<OCFLObjectSession> sessions = new ArrayList<>(this.sessionMap.values());

        //order in order of session creation time. (supports testing)
        Collections.sort(sessions, CREATION_TIME_ORDER);

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
        final List<OCFLObjectSession> committedSessions =
                this.sessionsToRollback.stream().map(c -> c.session).collect(Collectors.toList());
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
