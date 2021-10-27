/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateNonRdfSourceOperationBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;


/**
 * Builder for operations to create new non-rdf sources
 *
 * @author bbpennel
 */
public class CreateNonRdfSourceOperationBuilderImpl extends AbstractNonRdfSourceOperationBuilder
        implements CreateNonRdfSourceOperationBuilder {

    private FedoraId parentId;

    /**
     * Constructor for external binary.
     *
     * @param transaction the transaction
     * @param rescId      the internal identifier
     * @param handling    the external content handling type.
     * @param externalUri the external content URI.
     */
    protected CreateNonRdfSourceOperationBuilderImpl(final Transaction transaction, final FedoraId rescId,
                                                     final String handling,
                                                     final URI externalUri) {
        super(transaction, rescId, handling, externalUri);
    }

    /**
     * Constructor for internal binary.
     *
     * @param transaction the transaction
     * @param rescId the internal identifier.
     * @param stream the content stream.
     */
    protected CreateNonRdfSourceOperationBuilderImpl(final Transaction transaction, final FedoraId rescId,
                                                     final InputStream stream) {
        super(transaction, rescId, stream);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder mimeType(final String mimeType) {
        return (CreateNonRdfSourceOperationBuilder) super.mimeType(mimeType);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder filename(final String filename) {
        return (CreateNonRdfSourceOperationBuilder) super.filename(filename);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder contentDigests(final Collection<URI> digests) {
        return (CreateNonRdfSourceOperationBuilder) super.contentDigests(digests);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder contentSize(final long size) {
        return (CreateNonRdfSourceOperationBuilder) super.contentSize(size);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        return (CreateNonRdfSourceOperationBuilder) super.userPrincipal(userPrincipal);
    }

    @Override
    public CreateNonRdfSourceOperationBuilder parentId(final FedoraId parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public CreateNonRdfSourceOperation build() {
        final CreateNonRdfSourceOperation operation;
        if (externalURI != null && externalType != null) {
            operation = new CreateNonRdfSourceOperation(transaction, resourceId, externalURI, externalType);
        } else {
            operation = new CreateNonRdfSourceOperation(transaction, resourceId, content);
        }

        operation.setUserPrincipal(userPrincipal);
        operation.setDigests(digests);
        operation.setFilename(filename);
        operation.setContentSize(contentSize);
        operation.setMimeType(mimeType);
        operation.setParentId(parentId);

        return operation;
    }
}
