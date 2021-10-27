/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ReindexResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.REINDEX;


/**
 * Operation for reindexing a resource
 *
 * @author dbernstein
 */
public class ReindexResourceOperationImpl extends AbstractResourceOperation implements ReindexResourceOperation {

    private Transaction transaction;

    protected ReindexResourceOperationImpl(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public ResourceOperationType getType() {
        return REINDEX;
    }

    public void setTransaction(final Transaction tx) {
        transaction = tx;
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }
}
