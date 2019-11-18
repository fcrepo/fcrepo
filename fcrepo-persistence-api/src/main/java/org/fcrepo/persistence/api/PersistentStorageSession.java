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

import java.io.InputStream;
import java.time.Instant;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.ResourceOperation;
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
     * Perform a persistence operation on a resource
     *
     * @param operation The persistence operation to perform
     * @throws PersistentStorageException Error persisting the resource.
     */
    public void persist(final ResourceOperation operation)
            throws PersistentStorageException;

    /**
     * Get the header information for the identified resource.
     *
     * @param identifier identifier of the resource
     * @param version instant identifying the version of the resource to read from.
     *      If null, then the head version is used.
     * @return header information
     * @throws PersistentStorageException  Either a PersistentItemNotFoundException or PersistentSessionClosedException
     */
    public ResourceHeaders getHeaders(final String identifier, final Instant version)
            throws PersistentStorageException;

    /**
     * Get the client managed triples for the provided resource.
     *
     * @param identifier identifier for the resource.
     * @param version instant identifying the version of the resource to read from. If null, then the head version is
     *        used.
     * @return the triples as an RdfStream.
     * @throws PersistentStorageException  Either a PersistentItemNotFoundException or PersistentSessionClosedException
     */
    public RdfStream getTriples(final String identifier, final Instant version)
            throws PersistentStorageException;

    /**
     * Get the server managed properties for this provided resource.
     *
     * @param identifier identifier for the resource.
     * @param version instant identifying the version of the resource to read from. If null, then the head version is
     *        used.
     * @return the server managed properties as an RdfStream.
     * @throws PersistentStorageException  Either a PersistentItemNotFoundException or PersistentSessionClosedException
     */
    public RdfStream getManagedProperties(final String identifier, final Instant version)
            throws PersistentStorageException;

    /**
     * Get the persisted binary content for the provided resource.
     *
     * @param identifier identifier for the resource.
     * @param version instant identifying the version of the resource to read from. If null, then the head version is
     *        used.
     * @return the binary content.
     * @throws PersistentStorageException  Either a PersistentItemNotFoundException or PersistentSessionClosedException
     */
    public InputStream getBinaryContent(final String identifier, final Instant version)
            throws PersistentStorageException;

    /**
     * Commits any changes in the current sesssion to persistent storage.
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
