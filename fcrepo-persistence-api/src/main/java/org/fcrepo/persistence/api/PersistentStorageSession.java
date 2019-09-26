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
import java.util.List;

import org.fcrepo.kernel.api.models.Binary;
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
     * @param resource The new fedora resource to persist to storage.
     * @throws PersistentStorageException Error persisting the resource.
     */
    public void create(final FedoraResource resource)
            throws PersistentStorageException;

    /**
     * Update an existing resource on persistent storage.
     *
     * @param resource The changed fedora resource to persist to storage.
     * @throws PersistentStorageException Error persisting the resource.
     */
    public void update(final FedoraResource resource)
            throws PersistentStorageException;

    /**
     * Delete a resource from persistent storage.
     *
     * @param resource The current fedora resource to delete.
     * @throws PersistentStorageException Error deleting the resource.
     */
    public void delete(final FedoraResource resource)
            throws PersistentStorageException;

    /**
     * Return a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @return The resource.
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     * @throws PersistentStorageException If some other error happens.
     */
    public FedoraResource read(final String identifier) throws PersistentItemNotFoundException,
            PersistentStorageException;

    /**
     * Return a version of a resource from persistent storage
     *
     * @param identifier The identifier of the resource to retrieve.
     * @param version The date/time of the version to retrieve.
     * @return The version of the resource.
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     */
    public FedoraResource read(final String identifier, final Instant version) throws PersistentItemNotFoundException;

    /**
     * Returns the types for the selected resource.
     *
     * @param identifier identifier of the resource
     * @return list of types
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     */
    public List<String> getTypes(final String identifier) throws PersistentItemNotFoundException;

    /**
     * Read the client managed triples for the provided resource, and store them to that resource.
     *
     * @param resource the resource.
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     */
    public void readTriples(final FedoraResource resource) throws PersistentItemNotFoundException;

    /**
     * Read the server managed properties for this provided resource, and store them to the resource.
     *
     * @param resource the resource.
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     */
    public void readManagedProperties(final FedoraResource resource) throws PersistentItemNotFoundException;

    /**
     * Read the persisted binary content for the provided resource, and store it to the resource.
     *
     * @param resource the resource.
     * @throws PersistentItemNotFoundException If the identifier doesn't exist.
     */
    public void readBinaryContent(final Binary resource) throws PersistentItemNotFoundException;

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
