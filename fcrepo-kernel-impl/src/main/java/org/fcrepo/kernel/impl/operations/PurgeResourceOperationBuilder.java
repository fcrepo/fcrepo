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
 * Builder for operations to purge a resource
 *
 * @author whikloj
 */
public class PurgeResourceOperationBuilder extends AbstractResourceOperationBuilder
        implements ResourceOperationBuilder {

    /**
     * Construct the builder
     *
     * @param transaction the transaction
     * @param rescId identifier of the resource to delete
     */
    public PurgeResourceOperationBuilder(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public PurgeResourceOperation build() {
        final var operation = new PurgeResourceOperation(transaction, rescId);
        operation.setUserPrincipal(userPrincipal);
        return operation;
    }
}
