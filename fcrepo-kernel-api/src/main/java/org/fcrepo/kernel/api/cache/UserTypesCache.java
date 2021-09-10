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

package org.fcrepo.kernel.api.cache;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Cache of user RDF types. This cache has two levels. Types are cached at the session level as well as globally.
 * This is necessary to support long-running transactions that can span multiple requests.
 *
 * @author pwinckles
 */
public interface UserTypesCache {

    /**
     * Gets the user RDF types for the specified resource from the cache. First, the session's cache is checked.
     * If the types were not found, then the global cache is checked. If not in either cache, then the rdfProvider
     * is called to load the resource's RDF from which the types are parsed, cached, and returned.
     *
     * This method should NOT be called on binary resources.
     *
     * @param resourceId the id of the resource
     * @param sessionId the id of the current session
     * @param rdfProvider the provider that is called, if needed, to load the resource's rdf
     * @return the resource's user RDF types
     */
    List<URI> getUserTypes(final FedoraId resourceId,
                           final String sessionId,
                           final Supplier<RdfStream> rdfProvider);

    /**
     * Extracts the user RDF types from the RDF and caches them in the session level cache.
     *
     * @param resourceId the id of the resource
     * @param rdf the resource's RDF
     * @param sessionId the session to cache the types in
     */
    void cacheUserTypes(final FedoraId resourceId,
                        final RdfStream rdf,
                        final String sessionId);

    /**
     * Caches the user RDF types in the session level cache.
     *
     * @param resourceId the id of the resource
     * @param userTypes the resource's types
     * @param sessionId the session to cache the types in
     */
    void cacheUserTypes(final FedoraId resourceId,
                        final List<URI> userTypes,
                        final String sessionId);

    /**
     * Merges the session level cache into the global cache.
     *
     * @param sessionId the id of the session to merge
     */
    void mergeSessionCache(final String sessionId);

    /**
     * Drops a session level cache without merging it into the global cache.
     *
     * @param sessionId the id of the session cache to drop
     */
    void dropSessionCache(final String sessionId);

}
