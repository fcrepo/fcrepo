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

import java.io.InputStream;
import java.time.Instant;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

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

    /**
     * Constructor
     *
     * @param sessionId session id.
     */
    protected OCFLPersistentStorageSession(final String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Constructor
     */
    protected OCFLPersistentStorageSession() {
        this.sessionId = null;
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public void persist(final ResourceOperation operation) throws PersistentStorageException {
        // TODO Auto-generated method stub

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
