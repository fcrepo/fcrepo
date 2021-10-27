/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperationBuilder;
import org.springframework.stereotype.Component;

/**
 * Implementation of a delete resource operation factory
 *
 * @author bbpennel
 */
@Component
public class DeleteResourceOperationFactoryImpl implements DeleteResourceOperationFactory {

    @Override
    public DeleteResourceOperationBuilder deleteBuilder(final Transaction transaction, final FedoraId rescId) {
        return new DeleteResourceOperationBuilder(transaction, rescId);
    }

    @Override
    public ResourceOperationBuilder purgeBuilder(final Transaction transaction, final FedoraId rescId) {
        return new PurgeResourceOperationBuilder(transaction, rescId);
    }

}
