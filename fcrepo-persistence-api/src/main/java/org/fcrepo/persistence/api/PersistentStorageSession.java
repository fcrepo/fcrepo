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
import org.fcrepo.kernel.api.models.Tombstone;
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
     * @param resource The new fedora resource to persist to storage.
     * @return the resource with any new information related to the persisting..?
     * @throws PersistentStorageException Error persisting the resource.
     */
    public FedoraResource create(final FedoraResource resource)
            throws PersistentStorageException;

    /**
     * Update an existing resource on persistent storage.
     *
     * @param resource The changed fedora resource to persist to storage.
     * @return the updated resource with any new information related to the persisting..?
     * @throws PersistentStorageException Error persisting the resource.
     */
    public FedoraResource update(final FedoraResource resource)
            throws PersistentStorageException;

    /**
     * Delete a resource from persistent storage.
     *
     * @param resource The current fedora resource to delete.
     * @return The tombstone for the removed resource.
     * @throws PersistentStorageException Error deleting the resource.
     */
    public Tombstone delete(final FedoraResource resource)
            throws PersistentStorageException;

    /**
     * Return a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @return The resource.
     * @throws PersistentStorageException If the identifier doesn't exist.
     */
    public FedoraResource read(final String identifier)
            throws PersistentStorageException;

    /**
     * Return a version of a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @param version The date/time of the version to retrieve.
     * @return The version of the resource.
     * @throws PersistentStorageException If the identifier doesn't exist.
     */
    public FedoraResource read(final String identifier, final Instant version) throws PersistentStorageException;

    /**
     * Commits any changes in the current sesssion to persistent storage.
     *
     * @throws PersistentStorageException Error during commit.
     */
    public void commit() throws PersistentStorageException;

    /**
     * Rolls back any changes in the current session.
     *
     * @throws PersistentStorageException Error completing rollback.
     */
    public void rollback() throws PersistentStorageException;

}
