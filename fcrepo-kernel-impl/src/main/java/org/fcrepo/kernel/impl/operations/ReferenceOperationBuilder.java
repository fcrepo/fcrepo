/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Build a reference operation.
 * @author whikloj
 */
public class ReferenceOperationBuilder extends AbstractResourceOperationBuilder {

    /**
     * Constructor.
     *
     * @param transaction the transaction
     * @param rescId the resource identifier.
     */
    public ReferenceOperationBuilder(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public ReferenceOperation build() {
        final var operation = new ReferenceOperation(transaction, rescId);
        operation.setUserPrincipal(userPrincipal);
        return operation;
    }

}
