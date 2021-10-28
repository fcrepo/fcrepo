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
 * Purge resource operation
 *
 * @author whikloj
 * @since 6.0.0
 */
public class PurgeResourceOperation extends AbstractResourceOperation {

    protected PurgeResourceOperation(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public ResourceOperationType getType() {
        return ResourceOperationType.PURGE;
    }
}
