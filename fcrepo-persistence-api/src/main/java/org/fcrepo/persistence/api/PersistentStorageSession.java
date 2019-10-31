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
package org.fcrepo.persistence.api;

import java.time.Instant;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

/**
 * An interface that mediates CRUD operations to and from persistence storage.
 *
 * @author dbernstein
 * @author whikloj
 */
public interface PersistentStorageSession {

    /**
     * Return the ID for this session, or null for a read-only session.
     *
     * @return the session id.
     */
    public String getId();

    /**
     * Create a new resource on the persistent storage.
     *
     * @param request The request that includes all information required for creating a resource.
     * @return The create resource response.
     * @throws PersistentStorageException Error persisting the resource.
     */
    public CreateResourceResponse create(final CreateResourceRequest request)
        throws PersistentStorageException;

    /**
     * Update an existing resource on persistent storage.
     *
     * @param request The request that includes all information required for updating a resource.
     * @return The update resource response.
     * @throws PersistentStorageException Error persisting the resource.
     */
    public UpdateResourceResponse update(final UpdateResourceRequest request)
        throws PersistentStorageException;

    /**
     * Delete a resource from persistent storage.
     *
     * @param request The request that includes all information required for deleting a resource.
     * @return  The delete response
     * @throws PersistentStorageException Error deleting the resource.
     */
    public DeleteResourceResponse delete(final DeleteResourceRequest request)
        throws PersistentStorageException;

    /**
     * Return a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @return The resource.
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     * @throws PersistentStorageException      If some other error happens.
     */
    public FedoraResource read(final String identifier) throws PersistentItemNotFoundException,
        PersistentStorageException;

    /**
     * Return a version of a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @param version    The date/time of the version to retrieve.
     * @return The version of the resource.
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     */
    public FedoraResource read(final String identifier, final Instant version, ReadOptions... options) throws PersistentItemNotFoundException;

    /**
     * Commits any changes in the current sesssion to persistent storage.
     *
     * @throws PersistentStorageException Error during commit.
     */
    public void commit(CommitOption option) throws PersistentStorageException;

    /**
     * Rolls back any changes in the current session.
     *
     * @throws PersistentStorageException Error completing rollback.
     */
    public void rollback() throws PersistentStorageException;

}
