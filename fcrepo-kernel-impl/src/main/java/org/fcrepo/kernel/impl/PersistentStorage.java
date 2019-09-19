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
package org.fcrepo.kernel.impl;

import java.time.Instant;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Tombstone;

/**
 * Interface for interacting with a persistent storage implementation.
 *
 * @author whikloj
 * @since 2019-09-19
 */
public interface PersistentStorage {

    /**
     * Create a new resource on the persistent storage.
     *
     * @param psTxId The persistent storage transaction for this action.
     * @param resource The new fedora resource to persist to storage.
     * @return the resource with any new information related to the persisting..?
     */
    public FedoraResource create(final PersistentStorageTransaction psTxId, final FedoraResource resource);

    /**
     * Update an existing resource on persistent storage.
     *
     * @param psTxId The persistent storage transaction for this action.
     * @param resource The changed fedora resource to persist to storage.
     * @return the updated resource with any new information related to the persisting..?
     */
    public FedoraResource update(final PersistentStorageTransaction psTxId, final FedoraResource resource);

    /**
     * Delete a resource from persistent storage.
     *
     * @param psTxId The persistent storage transaction for this action.
     * @param resource The current fedora resource to delete.
     * @return The tombstone for the removed resource.
     */
    public Tombstone delete(final PersistentStorageTransaction psTxId, final FedoraResource resource);

    /**
     * Return a resource from persistent storage
     *
     * @param psTxId The persistent storage transaction for this action.
     * @param identifier The identifier of the resource to retrieve.
     * @return The resource.
     */
    public FedoraResource read(final PersistentStorageTransaction psTxId, final String identifier);

    /**
     * Return a version of a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @param version The date/time of the version to retrieve.
     * @return The version of the resource.
     */
    public FedoraResource read(final String identifier, final Instant version);

}
