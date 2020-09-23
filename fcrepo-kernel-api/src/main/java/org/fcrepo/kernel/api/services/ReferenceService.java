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
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * Service to retrieve references to repository resources.
 * @author whikloj
 * @since 6.0.0
 */
public interface ReferenceService {

    /**
     * Return a RDF stream of statements referring to the provided resource.
     *
     * @param txId the transaction ID or null if no transaction.
     * @param resource the resource to get inbound references for.
     * @return RDF stream of inbound reference triples.
     */
    RdfStream getInboundReferences(final String txId, final FedoraResource resource);

    /**
     * Delete all references from a resource to any other resource.
     *
     * @param txId the transaction ID
     * @param resourceId the ID of the resource referencing others.
     */
    void deleteAllReferences(final String txId, final FedoraId resourceId);

    /**
     * Parse the stream of triples for references, add any new ones and remove any missing ones.
     * @param txId the transaction ID
     * @param resourceId the subject ID of the triples.
     * @param userPrincipal the user who's action is updating references.
     * @param rdfStream the RDF stream.
     */
    void updateReferences(final String txId, final FedoraId resourceId, final String userPrincipal,
                          final RdfStream rdfStream);

    /**
     * Commit any pending references.
     * @param txId the transaction id.
     */
    void commitTransaction(final String txId);

    /**
     * Rollback any pending references.
     * @param txId the transaction id.
     */
    void rollbackTransaction(final String txId);

    /**
     * Truncates the reference index. This should only be called when rebuilding the index.
     */
    void reset();
}
