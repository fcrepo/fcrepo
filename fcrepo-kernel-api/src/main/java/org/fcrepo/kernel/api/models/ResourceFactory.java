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

import java.time.Instant;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;

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
     * @param identifier The path or identifier for the resource.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final String identifier)
            throws PathNotFoundException;

    /**
     * Get a FedoraResource for existing resource
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final Transaction transaction, final String identifier)
            throws PathNotFoundException;

    /**
     * Get a FedoraResource for existing resource
     *
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @param version The version datetime or null for head.
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public FedoraResource getResource(final Transaction transaction, final String identifier, final Instant version)
            throws PathNotFoundException;

    /**
     * Get a resource as a particular type without a transaction
     *
     * @param <T> type for the resource
     * @param identifier The path or identifier for the resource.
     * @param clazz class the resource will be cast to
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public <T extends FedoraResource> T getResource(final String identifier,
            final Class<T> clazz) throws PathNotFoundException;

    /**
     * Get a resource as a particular type
     *
     * @param <T> type for the resource
     * @param transaction The transaction this request is part of.
     * @param identifier The path or identifier for the resource.
     * @param version The version datetime or null for head.
     * @param clazz class the resource will be cast to
     * @return The resource.
     * @throws PathNotFoundException If the identifier cannot be found.
     */
    public <T extends FedoraResource> T getResource(final Transaction transaction, final String identifier,
            final Instant version, final Class<T> clazz) throws PathNotFoundException;

    /**
     * Get a resource as a particular type
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
     * Check if a resource exists.
     * @param transaction The current transaction or null if read-only.
     * @param fedoraId The internal identifier
     * @param version The version datetime or null for head.
     * @return True if the identifier resolves to a resource.
     */
    public boolean doesResourceExist(final Transaction transaction, final String fedoraId, final Instant version);
}
