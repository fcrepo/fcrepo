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
package org.fcrepo.kernel.api;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import javax.annotation.Nonnull;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * An interface for retrieving resource IDs by their containment relationships.
 *
 * @author dbernstein
 * @since 6.0.0
 */
public interface ContainmentIndex {

    /**
     * Return a stream of fedora identifiers contained by the specified fedora resource for the current state of the
     * repository.
     *
     * @param txId The transaction id, or null if no transaction
     * @param fedoraId The ID of the containing fedora resource
     * @return A stream of contained identifiers
     */
    Stream<String> getContains(String txId, FedoraId fedoraId);

    /**
     * Return a stream of fedora identifiers contained by the specified fedora resource that have deleted
     * relationships.
     *
     * @param txId The transaction id, or null if no transaction
     * @param fedoraId The ID of the containing fedora resource
     * @return A stream of contained identifiers
     */
    Stream<String> getContainsDeleted(String txId, FedoraId fedoraId);

    /**
     * Return the ID of the containing resource for resourceID.
     * @param txId The transaction id, or null if no transaction
     * @param resource The FedoraId of the resource to find the containing resource for.
     * @return The id of the containing resource or null if none found.
     */
    String getContainedBy(String txId, final FedoraId resource);

    /**
     * Mark a contained by relation between the child resource and its parent as deleted.
     *
     * @param txId The transaction ID.
     * @param parent The containing resource fedoraID.
     * @param child The contained resource fedoraID.
     */
    void removeContainedBy(@Nonnull final String txId, final FedoraId parent, final FedoraId child);

    /**
     * Mark all relationships to the specified resource as deleted.
     *
     * @param txId The transaction ID.
     * @param resource The FedoraId of resource to remove.
     */
    void removeResource(@Nonnull final String txId, final FedoraId resource);

    /**
     * Remove all relationships to the specified resource.
     *
     * @param txId The transaction ID.
     * @param resource The FedoraId of resource to remove.
     */
    void purgeResource(@Nonnull final String txId, final FedoraId resource);

    /**
     * Add a contained by relation between the child resource and its parent.
     *
     * @param txId The transaction ID.
     * @param parent The containing resource fedoraID.
     * @param child The contained resource fedoraID.
     */
    void addContainedBy(@Nonnull final String txId, final FedoraId parent, final FedoraId child);

    /**
     * Add a contained by relation between the child resource and its parent for a range of time in the past.
     *
     * @param txId The transaction ID.
     * @param parent The containing resource fedoraID.
     * @param child The contained resource fedoraID.
     * @param startTime The start instant of the containment relationship.
     * @param endTime The end instant of the containment relationship.
     */
    void addContainedBy(@Nonnull final String txId, final FedoraId parent, final FedoraId child,
                        final Instant startTime, final Instant endTime);

    /**
     * Commit the changes made in the transaction.
     * @param txId The transaction id.
     */
    void commitTransaction(final String txId);

    /**
     * Rollback the containment index changes in the transaction.
     * @param txId The transaction id.
     */
    void rollbackTransaction(final String txId);

    /**
     * Check if the resourceID exists in the containment index. Which should mean it exists.
     *
     * @param txId The transaction id, or null if no transaction
     * @param fedoraId The resource's FedoraId.
     * @param includeDeleted Include deleted resources in the search.
     * @return True if it is in the index.
     */
    boolean resourceExists(final String txId, final FedoraId fedoraId, final boolean includeDeleted);

    /**
     * Find the ID for the container of the provided resource by iterating up the path until you find a real resource.
     * @param txId The transaction id, or null if no transaction
     * @param fedoraId The resource's ID.
     * @param checkDeleted Whether to include deleted resource (tombstones) in the search.
     * @return The container ID.
     */
    FedoraId getContainerIdByPath(final String txId, final FedoraId fedoraId, final boolean checkDeleted);

    /**
     * Truncates the containment index. This should only be called when rebuilding the index.
     */
    void reset();

    /**
     * Find whether there are any resources that starts with the ID provided.
     * @param txId The transaction id, or null if no transaction.
     * @param fedoraId The ID to use to look for other IDs.
     * @return Are there any matching IDs.
     */
    boolean hasResourcesStartingWith(final String txId, final FedoraId fedoraId);

    /**
     * Find the timestamp of the last child added or deleted
     * @param txId The transaction id, or null if no transaction.
     * @param fedoraId The ID of the containing resource to check.
     * @return Timestamp of last child added or deleted or null if none
     */
    Instant containmentLastUpdated(final String txId, final FedoraId fedoraId);
}
