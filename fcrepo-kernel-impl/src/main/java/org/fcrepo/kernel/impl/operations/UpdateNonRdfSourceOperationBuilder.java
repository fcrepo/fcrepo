/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.io.InputStream;
import java.net.URI;

/**
 * Builder for operations to update non-rdf sources
 *
 * @author bbpennel
 */
public class UpdateNonRdfSourceOperationBuilder extends AbstractNonRdfSourceOperationBuilder {
    protected UpdateNonRdfSourceOperationBuilder(final Transaction transaction, final FedoraId rescId,
                                                 final InputStream stream) {
        super(transaction, rescId, stream);
    }

    protected UpdateNonRdfSourceOperationBuilder(final Transaction transaction, final FedoraId rescId,
                                                 final String handling,
                                                 final URI contentUri) {
        super(transaction, rescId, handling, contentUri);
    }

    @Override
    public UpdateNonRdfSourceOperation build() {
        final UpdateNonRdfSourceOperation operation;
        if (externalURI != null && externalType != null) {
            operation = new UpdateNonRdfSourceOperation(transaction, resourceId, externalURI, externalType);
        } else {
            operation = new UpdateNonRdfSourceOperation(transaction, resourceId, content);
        }

        operation.setUserPrincipal(userPrincipal);
        operation.setDigests(digests);
        operation.setFilename(filename);
        operation.setContentSize(contentSize);
        operation.setMimeType(mimeType);

        return operation;
    }
}
