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
 * Builder for operations to reindex a resource
 *
 * @author dbernstein
 */
public class ReindexResourceOperationBuilder extends AbstractResourceOperationBuilder
        implements ResourceOperationBuilder {

    /**
     * Construct the builder
     *
     * @param tx the transaction
     * @param rescId identifier of the resource to reindex
     */
    public ReindexResourceOperationBuilder(final Transaction tx, final FedoraId rescId) {
        super(tx, rescId);
    }

    @Override
    public ReindexResourceOperationImpl build() {
        final var operation = new ReindexResourceOperationImpl(transaction, rescId);
        operation.setUserPrincipal(userPrincipal);
        operation.setTransaction(transaction);
        return operation;
    }
}
