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

import java.time.Instant;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.CreateResourceRequest;
import org.fcrepo.persistence.api.CreateResourceResponse;
import org.fcrepo.persistence.api.DeleteResourceRequest;
import org.fcrepo.persistence.api.DeleteResourceResponse;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.ReadOptions;
import org.fcrepo.persistence.api.UpdateResourceRequest;
import org.fcrepo.persistence.api.UpdateResourceResponse;
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
    public CreateResourceResponse create(final CreateResourceRequest request) throws PersistentStorageException {
        actionNeedsWrite();
        // Do stuff to persist this new resource.
        return null;
    }

    @Override
    public UpdateResourceResponse update(final UpdateResourceRequest request) throws PersistentStorageException {
        actionNeedsWrite();
        // Update the resource in peristent storage.
        return null;
    }

    @Override
    public DeleteResourceResponse delete(final DeleteResourceRequest request) throws PersistentStorageException {
        actionNeedsWrite();
        // Delete the resource from storage.
        return null;
    }

    @Override
    public FedoraResource read(final String identifier) throws PersistentStorageException,
            PersistentItemNotFoundException {
        actionNeedsWrite();
        return null;
    }

    @Override
    public FedoraResource read(final String identifier, final Instant version, final ReadOptions... options)
            throws PersistentItemNotFoundException {
        return null;
    }

    @Override
    public void commit(final CommitOption option) throws PersistentStorageException {
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
