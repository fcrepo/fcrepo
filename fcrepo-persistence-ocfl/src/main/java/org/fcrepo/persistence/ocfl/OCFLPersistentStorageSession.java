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

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.api.Persister;
import org.fcrepo.persistence.ocfl.impl.DeleteResourcePersister;
import org.fcrepo.persistence.ocfl.impl.FedoraOCFLMapping;
import org.fcrepo.persistence.ocfl.impl.NonRdfSourcePersister;
import org.fcrepo.persistence.ocfl.impl.RDFSourcePersister;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getRDFFileExtension;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getRdfStream;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.relativizeSubpath;

/**
 * OCFL Persistent Storage class.
 *
 * @author whikloj
 * @since 2019-09-20
 */
public class OCFLPersistentStorageSession implements PersistentStorageSession {

    private static final String FEDORA_METADATA_SUFFIX = "/fcr:metadata";

    /**
     * Externally generated id for the session.
     */
    private final String sessionId;

    private final FedoraToOCFLObjectIndex fedoraOcflIndex;

    private final Map<String, OCFLObjectSession> sessionMap;

    private final ReentrantLock mutex = new ReentrantLock();

    private final static List<Persister> PERSISTER_LIST = new LinkedList<>();

    static {
        PERSISTER_LIST.add(new RDFSourcePersister());
        PERSISTER_LIST.add(new NonRdfSourcePersister());
        PERSISTER_LIST.add(new DeleteResourcePersister());
        //TODO add new persisters here as they are implemented.
    }

    private OCFLObjectSessionFactory objectSessionFactory;

    /**
     * Constructor
     *
     * @param sessionId       session id.
     * @param fedoraOcflIndex the index
     * @param objectSessionFactory the session factory
     */
    protected OCFLPersistentStorageSession(final String sessionId, final FedoraToOCFLObjectIndex fedoraOcflIndex,
                                           final OCFLObjectSessionFactory objectSessionFactory) {
        this.sessionId = sessionId;
        this.fedoraOcflIndex = fedoraOcflIndex;
        this.objectSessionFactory = objectSessionFactory;
        this.sessionMap = new HashMap<>();
    }

    /**
     * Constructor
     *
     * @param fedoraOcflIndex the index
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

        try {
            mutex.lock();

            //resolve the mapping between the fedora resource identifier and the associated OCFL object.
            final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(operation.getResourceId());

            //get the session.
            final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());

            //resolve the persister based on the operation
            final Persister persister = PERSISTER_LIST.stream().filter(p -> p.handle(operation)).findFirst().orElse(null);

            if (persister == null) {
                throw new UnsupportedOperationException(format("The %s is not yet supported", operation.getClass()));
            }

            //perform the operation
            persister.persist(objSession, operation, mapping);

        } finally {
            mutex.unlock();
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
    public ResourceHeaders getHeaders(final String identifier, final Instant version) throws PersistentItemNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RdfStream getTriples(final String identifier, final Instant version)
            throws PersistentStorageException {
        final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(identifier);
        final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());
        final String fedoraSubpath = relativizeSubpath(mapping.getParentFedoraResourceId(), identifier);
        final String ocflSubpath = resolveOCFLSubpath(fedoraSubpath);
        final String filePath = ocflSubpath + getRDFFileExtension();
        return getRdfStream(identifier, version, objSession, filePath);
    }

    private String resolveOCFLSubpath(final String fedoraSubpath) {
        if (fedoraSubpath.endsWith(FEDORA_METADATA_SUFFIX)) {
            return fedoraSubpath.substring(FEDORA_METADATA_SUFFIX.length());
        } else {
            return fedoraSubpath;
        }
    }

    @Override
    public RdfStream getManagedProperties(final String identifier, final Instant version)
            throws PersistentStorageException {
        final FedoraOCFLMapping mapping = fedoraOcflIndex.getMapping(identifier);
        final OCFLObjectSession objSession = findOrCreateSession(mapping.getOcflObjectId());
        final String fedoraSubpath = relativizeSubpath(mapping.getParentFedoraResourceId(), identifier);
        final String ocflSubpath = resolveOCFLSubpath(fedoraSubpath);
        final String filePath = getInternalFedoraDirectory() + ocflSubpath + getRDFFileExtension();
        return getRdfStream(identifier, version, objSession, filePath);
    }

    @Override
    public InputStream getBinaryContent(final String identifier, final Instant version) throws PersistentItemNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void commit(final CommitOption option) throws PersistentStorageException {
        if (isReadOnly()) {
            // No changes to commit.
            return;
        }

        try {
            mutex.lock();
            //prepare session for commit
            final Collection<OCFLObjectSession> sessions = this.sessionMap.values();

            for(OCFLObjectSession objectSession : sessions){
                objectSession.prepare();
            }

            //perform commit
            for(OCFLObjectSession objectSession : sessions){
                objectSession.commit(option);
            }

            //close each session
            for(OCFLObjectSession objectSession : sessions){
                objectSession.close();
            }

            //purge object sessions
            sessionMap.clear();

        } finally {
            mutex.unlock();
        }
    }

    @Override
    public void rollback() throws PersistentStorageException {
        if (isReadOnly()) {
            // No changes to rollback
            return;
        }
        // rollback changes
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
