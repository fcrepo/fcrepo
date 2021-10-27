/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.DELETE;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperationType;


/**
 * Operation for deleting a resource
 *
 * @author bbpennel
 */
public class DeleteResourceOperation extends AbstractResourceOperation {

    protected DeleteResourceOperation(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

    @Override
    public ResourceOperationType getType() {
        return DELETE;
    }
}
