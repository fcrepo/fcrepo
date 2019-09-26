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
package org.fcrepo.kernel.api.models;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;

/**
 * Interface to a factory to locate or create new resources.
 *
 * @author whikloj
 * @since 2019-09-23
 */
public interface ResourceFactory {

    /**
     * Get a resource from the persistence layer.
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final Transaction transaction, final String identifier)
            throws PathNotFoundException;

    /**
     * Get a resource from the persistence layer as a particular type
     *
     * @param <T> type for the resource
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @param clazz class the resource will be cast to
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public <T extends FedoraResource> T getResource(final Transaction transaction, final String identifier,
            final Class<T> clazz) throws PathNotFoundException;

    /**
     * Create a new container.
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @return The container object.
     */
    public Container createContainer(final Transaction transaction, final String identifier);

    /**
     * Create a new binary. Newly created binaries will have nothing backing them until they are populated by a
     * service.
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @return The binary object.
     */
    public Binary createBinary(final Transaction transaction, final String identifier);

    /**
     * Create a binary description.
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @return The description object.
     */
    public NonRdfSourceDescription createBinaryDescription(final Transaction transaction, final String identifier);

    /**
     * Create a timemap.
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier of the resource this timemap is linked to.
     * @return The timemap object.
     */
    public TimeMap createTimemap(final Transaction transaction, final String identifier);

    /**
     * Create a Webac ACL.
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier of the resource this ACL is linked to.
     * @return The ACL object.
     */
    public WebacAcl createAcl(final Transaction transaction, final String identifier);

    /*
     * TODO: Do we need a model for an archival group?
     * public ArchivalGroup createArchivalGroup(final FedoraTransaction transaction, final String identifier);
     */

}
