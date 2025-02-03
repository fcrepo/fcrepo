/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
     * @param tx The transaction, or null if no transaction
     * @param fedoraId The ID of the containing fedora resource
     * @return A stream of contained identifiers
     */
    Stream<String> getContains(Transaction tx, FedoraId fedoraId);

    /**
     * Return a stream of fedora identifiers contained by the specified fedora resource that have deleted
     * relationships.
     *
     * @param tx The transaction, or null if no transaction
     * @param fedoraId The ID of the containing fedora resource
     * @return A stream of contained identifiers
     */
    Stream<String> getContainsDeleted(Transaction tx, FedoraId fedoraId);

    /**
     * Return the ID of the containing resource for resourceID.
     * @param tx The transaction, or null if no transaction
     * @param resource The FedoraId of the resource to find the containing resource for.
     * @return The id of the containing resource or null if none found.
     */
    String getContainedBy(Transaction tx, final FedoraId resource);

    /**
     * Mark a contained by relation between the child resource and its parent as deleted.
     *
     * @param tx The transaction.
     * @param parent The containing resource fedoraID.
     * @param child The contained resource fedoraID.
     */
    void removeContainedBy(@Nonnull final Transaction tx, final FedoraId parent, final FedoraId child);

    /**
     * Mark all relationships to the specified resource as deleted.
     *
     * @param tx The transaction.
     * @param resource The FedoraId of resource to remove.
     */
    void removeResource(@Nonnull final Transaction tx, final FedoraId resource);

    /**
     * Remove all relationships to the specified resource.
     *
     * @param tx The transaction.
     * @param resource The FedoraId of resource to remove.
     */
    void purgeResource(@Nonnull final Transaction tx, final FedoraId resource);

    /**
     * Add a contained by relation between the child resource and its parent.
     *
     * @param tx The transaction.
     * @param parent The containing resource fedoraID.
     * @param child The contained resource fedoraID.
     */
    void addContainedBy(@Nonnull final Transaction tx, final FedoraId parent, final FedoraId child);

    /**
     * Add a contained by relation between the child resource and its parent for a range of time in the past.
     *
     * @param tx The transaction.
     * @param parent The containing resource fedoraID.
     * @param child The contained resource fedoraID.
     * @param startTime The start instant of the containment relationship.
     * @param endTime The end instant of the containment relationship.
     */
    void addContainedBy(@Nonnull final Transaction tx, final FedoraId parent, final FedoraId child,
                        final Instant startTime, final Instant endTime);

    /**
     * Commit the changes made in the transaction.
     * @param tx The transaction.
     */
    void commitTransaction(final Transaction tx);

    /**
     * Rollback the containment index changes in the transaction.
     * @param tx The transaction.
     */
    void rollbackTransaction(final Transaction tx);

    /**
     * Clear all transactions in the containment index.
     */
    void clearAllTransactions();

    /**
     * Check if the resourceID exists in the containment index. Which should mean it exists.
     *
     * @param tx The transaction, or null if no transaction
     * @param fedoraId The resource's FedoraId.
     * @param includeDeleted Include deleted resources in the search.
     * @return True if it is in the index.
     */
    boolean resourceExists(final Transaction tx, final FedoraId fedoraId, final boolean includeDeleted);

    /**
     * Find the ID for the container of the provided resource by iterating up the path until you find a real resource.
     * @param tx The transaction, or null if no transaction
     * @param fedoraId The resource's ID.
     * @param checkDeleted Whether to include deleted resource (tombstones) in the search.
     * @return The container ID.
     */
    FedoraId getContainerIdByPath(final Transaction tx, final FedoraId fedoraId, final boolean checkDeleted);

    /**
     * Truncates the containment index. This should only be called when rebuilding the index.
     */
    void reset();

    /**
     * Find whether there are any resources that starts with the ID provided.
     * @param tx The transaction, or null if no transaction.
     * @param fedoraId The ID to use to look for other IDs.
     * @return Are there any matching IDs.
     */
    boolean hasResourcesStartingWith(final Transaction tx, final FedoraId fedoraId);

    /**
     * Find the timestamp of the last child added or deleted
     * @param tx The transaction, or null if no transaction.
     * @param fedoraId The ID of the containing resource to check.
     * @return Timestamp of last child added or deleted or null if none
     */
    Instant containmentLastUpdated(final Transaction tx, final FedoraId fedoraId);
}
