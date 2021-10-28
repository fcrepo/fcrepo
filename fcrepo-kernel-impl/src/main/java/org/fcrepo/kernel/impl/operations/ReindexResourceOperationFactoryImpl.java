/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ReindexResourceOperationFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of a reindex resource operation factory
 *
 * @author dbernstein
 */
@Component
public class ReindexResourceOperationFactoryImpl implements ReindexResourceOperationFactory {

    @Override
    public ReindexResourceOperationBuilder create(final Transaction transaction, final FedoraId resourceId) {
        return new ReindexResourceOperationBuilder(transaction, resourceId);
    }
}
