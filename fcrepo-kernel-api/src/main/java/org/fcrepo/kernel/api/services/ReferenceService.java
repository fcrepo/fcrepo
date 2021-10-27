/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
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
     * @param tx the transaction or null if no transaction.
     * @param resource the resource to get inbound references for.
     * @return RDF stream of inbound reference triples.
     */
    RdfStream getInboundReferences(final Transaction tx, final FedoraResource resource);

    /**
     * Delete all references from a resource to any other resource.
     *
     * @param tx the transaction
     * @param resourceId the ID of the resource referencing others.
     */
    void deleteAllReferences(final Transaction tx, final FedoraId resourceId);

    /**
     * Parse the stream of triples for references, add any new ones and remove any missing ones.
     * @param tx the transaction
     * @param resourceId the subject ID of the triples.
     * @param userPrincipal the user who's action is updating references.
     * @param rdfStream the RDF stream.
     */
    void updateReferences(final Transaction tx, final FedoraId resourceId, final String userPrincipal,
                          final RdfStream rdfStream);

    /**
     * Commit any pending references.
     * @param tx the transaction.
     */
    void commitTransaction(final Transaction tx);

    /**
     * Rollback any pending references.
     * @param tx the transaction.
     */
    void rollbackTransaction(final Transaction tx);

    /**
     * Truncates the reference index. This should only be called when rebuilding the index.
     */
    void reset();
}
