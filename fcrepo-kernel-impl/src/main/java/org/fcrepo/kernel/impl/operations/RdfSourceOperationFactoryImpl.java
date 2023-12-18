/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.springframework.stereotype.Component;


/**
 * Implementation of a factory for operations on rdf sources
 *
 * @author bbpennel
 */
@Component
public class RdfSourceOperationFactoryImpl implements RdfSourceOperationFactory {

    @Override
    public CreateRdfSourceOperationBuilder createBuilder(final Transaction transaction,
                                                         final FedoraId rescId,
                                                         final String interactionModel,
                                                         final ServerManagedPropsMode serverManagedPropsMode) {
        return createBuilder(transaction, rescId, interactionModel, serverManagedPropsMode, false);
    }

    @Override
    public CreateRdfSourceOperationBuilder createBuilder(final Transaction transaction, final FedoraId rescId,
                                                         final String interactionModel,
                                                         final ServerManagedPropsMode serverManagedPropsMode,
                                                         final boolean isOverwrite) {
        return new CreateRdfSourceOperationBuilderImpl(transaction, rescId, interactionModel, serverManagedPropsMode,
                                                       isOverwrite);
    }

    @Override
    public RdfSourceOperationBuilder updateBuilder(final Transaction transaction,
                                                   final FedoraId rescId,
                                                   final ServerManagedPropsMode serverManagedPropsMode) {
        return new UpdateRdfSourceOperationBuilder(transaction, rescId, serverManagedPropsMode);
    }
}
