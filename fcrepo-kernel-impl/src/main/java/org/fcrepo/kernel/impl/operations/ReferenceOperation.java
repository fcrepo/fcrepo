/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperationType;

/**
 * Operation to track a reference operation.
 * @author whikloj
 */
public class ReferenceOperation extends AbstractResourceOperation {

    protected ReferenceOperation(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public ResourceOperationType getType() {
        return ResourceOperationType.FOLLOW;
    }
}
