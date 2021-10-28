/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateNonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperationBuilder;
import org.springframework.stereotype.Component;

/**
 * Factory for operations to update non-rdf sources
 *
 * @author bbpennel
 */
@Component
public class NonRdfSourceOperationFactoryImpl implements NonRdfSourceOperationFactory {

    @Override
    public UpdateNonRdfSourceOperationBuilder updateExternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
                                                                          final String handling,
                                                                          final URI contentUri) {
        return new UpdateNonRdfSourceOperationBuilder(transaction, rescId, handling, contentUri);
    }

    @Override
    public UpdateNonRdfSourceOperationBuilder updateInternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
                                                                    final InputStream contentStream) {
        return new UpdateNonRdfSourceOperationBuilder(transaction, rescId, contentStream);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder createExternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
            final String handling, final URI contentUri) {
        return new CreateNonRdfSourceOperationBuilderImpl(transaction, rescId, handling, contentUri);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder createInternalBinaryBuilder(final Transaction transaction,
                                                                          final FedoraId rescId,
            final InputStream contentStream) {
        return new CreateNonRdfSourceOperationBuilderImpl(transaction, rescId, contentStream);
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder updateHeadersBuilder(final Transaction transaction,
                                                               final FedoraId rescId,
                                                               final ServerManagedPropsMode serverManagedPropsMode) {
        return new UpdateNonRdfSourceHeadersOperationBuilderImpl(transaction, rescId, serverManagedPropsMode);
    }
}
