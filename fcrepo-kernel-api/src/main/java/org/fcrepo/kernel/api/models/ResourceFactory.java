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

import java.util.stream.Stream;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Interface to a factory to instantiate FedoraResources
 *
 * @author whikloj
 * @since 2019-09-23
 */
public interface ResourceFactory {

    /**
     * Get a FedoraResource for existing resource without using a transaction.
     *
     * @param fedoraID The path or identifier for the resource.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final FedoraId fedoraID)
            throws PathNotFoundException;

    /**
     * Get a FedoraResource for existing resource
     *
     * @param transaction The transaction associated with this request or null if not in a transaction.
     * @param fedoraID The identifier for the resource.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final Transaction transaction, final FedoraId fedoraID)
            throws PathNotFoundException;

    /**
     * Get a FedoraResource for existing resource
     *
     * @param transactionId The transaction id associated with this request or null if not in a transaction.
     * @param fedoraID The identifier for the resource.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final String transactionId, final FedoraId fedoraID)
            throws PathNotFoundException;

    /**
     * Get a resource as a particular type without a transaction
     *
     * @param <T> type for the resource
     * @param fedoraID The identifier for the resource.
     * @param clazz class the resource will be cast to
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public <T extends FedoraResource> T getResource(final FedoraId fedoraID,
            final Class<T> clazz) throws PathNotFoundException;

    /**
     * Get a resource as a particular type
     *
     * @param <T> type for the resource
     * @param transaction The transaction associated with this request or null
     * @param fedoraID The identifier for the resource.
     * @param clazz class the resource will be cast to
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public <T extends FedoraResource> T getResource(final Transaction transaction, final FedoraId fedoraID,
                                                    final Class<T> clazz) throws PathNotFoundException;

    /**
     * Get the containing resource (if exists).
     * @param transactionId The current transaction id
     * @param resourceId The internal identifier
     * @return The containing resource or null if none.
     */
    public FedoraResource getContainer(final String transactionId, final FedoraId resourceId);

    /**
     * Get immediate children of the resource
     * @param transactionId The transaction id
     * @param resourceId Identifier of the resource
     * @return Stream of child resources
     */
    public Stream<FedoraResource> getChildren(final String transactionId, final FedoraId resourceId);
}
