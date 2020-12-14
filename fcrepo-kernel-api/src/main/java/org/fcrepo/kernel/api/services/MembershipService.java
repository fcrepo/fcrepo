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

import java.time.Instant;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Service used to manage membership properties of resources
 *
 * @author bbpennel
 */
public interface MembershipService {

    /**
     * Return an RdfStream of membership relations of which the provided resource is the subject.
     *
     * @param txId transaction id
     * @param fedoraId the resource to get membership relations for.
     * @return RdfStream of membership relations.
     */
    RdfStream getMembership(final String txId, final FedoraId fedoraId);

    /**
     * Update membership properties based on the creation of the specified resource
     *
     * @param txId transaction id
     * @param fedoraId ID of the object created
     */
    void resourceCreated(final String txId, final FedoraId fedoraId);

    /**
     * Update membership properties based on the modification of the specified resource
     *
     * @param txId transaction id
     * @param fedoraId ID of the object modified
     */
    void resourceModified(final String txId, final FedoraId fedoraId);

    /**
     * Update membership properties based on the deletion of the specified resource
     *
     * @param txId transaction id
     * @param fedoraId ID of the object deleted
     */
    void resourceDeleted(final String txId, final FedoraId fedoraId);

    /**
     * Regenerate the membership history for specified Direct or Indirect container.
     *
     * @param txId transaction id
     * @param containerId ID of the container
     */
    void populateMembershipHistory(final String txId, final FedoraId containerId);

    /**
     * Get the timestamp of the most recent member added or removed, or null if none.
     * @param txId transaction id or null if none
     * @param fedoraId the resource id
     * @return the timestamp or null
     */
    Instant getLastUpdatedTimestamp(final String txId, final FedoraId fedoraId);

    /**
     * Commit any pending membership changes.
     * @param txId the transaction id.
     */
    void commitTransaction(final String txId);

    /**
     * Rollback any pending membership changes.
     * @param txId the transaction id.
     */
    void rollbackTransaction(final String txId);

    /**
     * Truncates the membership index. This should only be called when rebuilding the index.
     */
    void reset();
}
