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

/**
 * An interface that mediates CRUD operations to and from persistence storage.
 *
 * @author dbernstein
 * @author whikloj
 * @since 2019-09-11
 */
public interface PersistentStorage {

    /**
     * Locate an existing resource on disk.
     *
     * @param txID The transaction identifer or null if no transaction.
     * @param identifier The id of the resource.
     * @param version The datetime of the version you want to retrieve or null if current.
     * @return the FedoraResource
     */
    public FedoraResource findResource(final String txID, final String identifier, final Instant version);

    /**
     * Save/Update a Resource to storage.
     *
     * @param txID The transaction identifier or null if no transaction.
     * @param identifier The identifier of the resource (if known).
     * @param resource The FedoraResource to persist.
     * @return the identifier
     */
    public String saveResource(final String txID, final String identifier, final FedoraResource resource);

    /**
     * Delete an object from storage.
     *
     * @param txID The transaction identifer or null if no transaction.
     * @param identifier The identifier of the resource.
     * @param version The datetime of the version to be deleted or null for entire resource.
     */
    public void deleteResource(final String txID, final String identifier, final Instant version);

    /**
     * Start a new transaction.
     *
     * @param idleTimeout The amount of time to wait between actions in the transaction.
     * @return the identifier of the transaction.
     */
    public String startTransaction(final long idleTimeout);

    /**
     * Commit a transaction.
     *
     * @param identifier The identifier of the transaction.
     */
    public void commitTransaction(final String identifier);

    /**
     * Rollback a transaction.
     *
     * @param identifier The identifier of the transaction.
     */
    public void rollbackTransaction(final String identifier);

    /**
     * Extend a transaction by its idleTimeout length.
     *
     * @param identifier The identifier of the transaction.
     */
    public void extendTransaction(final String identifier);
}
