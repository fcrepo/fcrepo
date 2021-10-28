/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;


import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Factory for delete resource operations
 *
 * @author bbpennel
 */
public interface DeleteResourceOperationFactory extends ResourceOperationFactory {

    /**
     * Get a builder for an operation to delete a resource
     *
     * @param transaction the transaction
     * @param rescId id of the resource to delete
     * @return new builder
     */
    ResourceOperationBuilder deleteBuilder(Transaction transaction, FedoraId rescId);

    /**
     * Get a builder for an operation to purge a deleted resource.
     *
     * @param transaction the transaction
     * @param rescId id of the resource to purge
     * @return new builder
     */
    ResourceOperationBuilder purgeBuilder(Transaction transaction, FedoraId rescId);
}
