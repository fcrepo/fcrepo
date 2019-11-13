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

import java.io.File;
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
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.Persister;
import org.fcrepo.persistence.ocfl.impl.DefaultOCFLObjectSession;
import org.fcrepo.persistence.ocfl.impl.FedoraOCFLMapping;
import org.fcrepo.persistence.ocfl.impl.RDFSourcePersister;

/**
 * OCFL Persistent Storage class.
 *
 * @author whikloj
 * @since 2019-09-20
 */
public class OCFLPersistentStorageSession implements PersistentStorageSession {

    /**
     * Externally generated id for the session.
     */
    private final String sessionId;

    private final FedoraToOCFLObjectIndex fedoraOcflIndex;

    private final Map<String, OCFLObjectSession> sessionMap;

    //TODO make the stagingPathRoot configurable.
    private final File stagingPathRoot = new File(System.getProperty("java.io.tmpdir"), "ocfl-staging");

    private final static List<Persister> PERSISTER_LIST = new LinkedList<>();

    static {
        PERSISTER_LIST.add(new RDFSourcePersister());
        //TODO add new persisters here as they are implemented.
    }

    /**
     * Constructor
     *
     * @param sessionId session id.
     * @param fedoraOcflIndex the index
     */
    protected OCFLPersistentStorageSession(final String sessionId, final FedoraToOCFLObjectIndex fedoraOcflIndex) {
        this.sessionId = sessionId;
        this.fedoraOcflIndex = fedoraOcflIndex;
        this.sessionMap = new HashMap<>();
        stagingPathRoot.mkdirs();
    }

    /**
     * Constructor
     * @param fedoraOcflIndex  the index
     */
    protected OCFLPersistentStorageSession(final FedoraToOCFLObjectIndex fedoraOcflIndex) {
        this(null, fedoraOcflIndex);
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public void persist(final ResourceOperation operation) throws PersistentStorageException {
        actionNeedsWrite();

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
    }

    private OCFLObjectSession findOrCreateSession(final String ocflId) {
        final OCFLObjectSession sessionObj = this.sessionMap.get(ocflId);
        if (sessionObj != null) {
            return sessionObj;
        }

        final File stagingDirectory = new File(stagingPathRoot, sessionId);
        stagingDirectory.mkdir();
        final OCFLObjectSession newSession = new DefaultOCFLObjectSession(ocflId, stagingDirectory.toPath(), null);
        sessionMap.put(ocflId, newSession);
        return newSession;
    }

    @Override
    public ResourceHeaders getHeaders(final String identifier, final Instant version) throws PersistentItemNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RdfStream getTriples(final String identifier, final Instant version) throws PersistentItemNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RdfStream getManagedProperties(final String identifier, final Instant version) throws PersistentItemNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getBinaryContent(final String identifier, final Instant version) throws PersistentItemNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void commit() throws PersistentStorageException {
        if (isReadOnly()) {
            // No changes to commit.
            return;
        }
        // commit changes.
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
