/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.api;

import jakarta.annotation.Nonnull;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.impl.FedoraOcflMapping;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public interface FedoraToOcflObjectIndex {

    /**
     * Retrieve identification information for the OCFL object which either contains, or is identified by,
     * the provided fedora resource id. In other words the method will find the closest resource that is persisted
     * as an OCFL object and returns its identifiers.
     *
     * If you pass fedora identifier that is not part of an archival group such as
     * "my/fedora/binary/fcr:metadata"  the  fedora resource returned in the mapping will be "my/fedora/binary".
     *
     * Contrast this  with an Archival Group example:  if you pass in "my/archival-group/binary/fcr:metadata" the
     * resource returned in the mapping would be "my/archival-group".
     *
     * @param session the current session, or null for read-only.
     * @param fedoraResourceIdentifier the fedora resource identifier
     *
     * @return the mapping
     * @throws FedoraOcflMappingNotFoundException when no mapping exists for the specified identifier.
     */
    FedoraOcflMapping getMapping(final Transaction session, final FedoraId fedoraResourceIdentifier)
            throws FedoraOcflMappingNotFoundException;

    /**
     * Adds a mapping to the index
     *
     * @param session the current session.
     * @param fedoraResourceIdentifier The fedora resource
     * @param fedoraRootObjectIdentifier   The identifier of the root fedora object resource
     * @param ocflObjectId             The ocfl object id
     * @return  The newly created mapping
     */
    FedoraOcflMapping addMapping(@Nonnull Transaction session, final FedoraId fedoraResourceIdentifier,
                                 final FedoraId fedoraRootObjectIdentifier, final String ocflObjectId);

    /**
     * Removes a mapping
     *
     * @param session the current session.
     * @param fedoraResourceIdentifier The fedora resource to remove the mapping for
     */
    void removeMapping(@Nonnull final Transaction session, final FedoraId fedoraResourceIdentifier);

    /**
     * Remove all persistent state associated with the index.
     */
    void reset();

    /**
     * Commit mapping changes for the session.
     *
     * @param session the session to commit.
     */
    void commit(@Nonnull final Transaction session);

    /**
     * Rollback mapping changes for the session.
     *
     * @param session the session to rollback.
     */
    void rollback(@Nonnull final Transaction session);

    /**
     * Clear all transactions in the ocfl index.
     */
    void clearAllTransactions();
}

