/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;


import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Factory for reindex resource operations
 *
 * @author dbernstein
 */
public interface ReindexResourceOperationFactory extends ResourceOperationFactory {

    /**
     * Get a builder for an operation to reindex a resource
     *
     * @param transaction the transaction
     * @param resourceId id of the resource to reindex
     * @return new builder
     */
    ResourceOperationBuilder create(Transaction transaction, FedoraId resourceId);
}
