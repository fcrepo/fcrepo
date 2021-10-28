/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperationBuilder;


/**
 * Builder for operations to delete a resource
 *
 * @author bbpennel
 */
public class DeleteResourceOperationBuilder extends AbstractResourceOperationBuilder
        implements ResourceOperationBuilder {

    /**
     * Construct the builder
     *
     * @param transaction the transaction
     * @param rescId identifier of the resource to delete
     */
    public DeleteResourceOperationBuilder(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public DeleteResourceOperation build() {
        final var operation = new DeleteResourceOperation(transaction, rescId);
        operation.setUserPrincipal(userPrincipal);
        return operation;
    }
}
