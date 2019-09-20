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

import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Tombstone;

/**
 * An abstract class that mediates CRUD operations to and from persistence storage.
 *
 * @author dbernstein
 * @author whikloj
 */
abstract public class PersistentStorageSession {

    /**
     * The FedoraTransaction ID.
     */
    private final String txId;

    /**
     * Basic constructor.
     *
     * @param txId the FedoraTransaction ID.
     */
    protected PersistentStorageSession(final String txId) {
        this.txId = txId;
    }

    /**
     * Create a new resource on the persistent storage.
     *
     * @param resource The new fedora resource to persist to storage.
     * @return the resource with any new information related to the persisting..?
     * @throws CannotCreateResourceException Error persisting the resource.
     */
    abstract public FedoraResource create(final FedoraResource resource)
            throws CannotCreateResourceException;

    /**
     * Update an existing resource on persistent storage.
     *
     * @param resource The changed fedora resource to persist to storage.
     * @return the updated resource with any new information related to the persisting..?
     * @throws CannotCreateResourceException Error persisting the resource.
     */
    abstract public FedoraResource update(final FedoraResource resource)
            throws CannotCreateResourceException;

    /**
     * Delete a resource from persistent storage.
     *
     * @param resource The current fedora resource to delete.
     * @return The tombstone for the removed resource.
     * @throws RepositoryRuntimeException Error deleting the resource.
     */
    abstract public Tombstone delete(final FedoraResource resource)
            throws RepositoryRuntimeException;

    /**
     * Return a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @return The resource.
     * @throws ItemNotFoundException If the identifier doesn't exist.
     */
    abstract public FedoraResource read(final String identifier)
            throws ItemNotFoundException;

    /**
     * Return a version of a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @param version The date/time of the version to retrieve.
     * @return The version of the resource.
     * @throws ItemNotFoundException If the identifier doesn't exist.
     */
    abstract public FedoraResource read(final String identifier, final Instant version) throws ItemNotFoundException;

}
