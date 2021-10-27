/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperation;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperationBuilder;

import java.net.URI;
import java.util.Collection;

/**
 * Builder for an operation for updating headers of non-rdf sources
 *
 * @author mikejritter
 * @author bbpennel
 */
public class UpdateNonRdfSourceHeadersOperationBuilderImpl extends AbstractRelaxableResourceOperationBuilder
                                                           implements UpdateNonRdfSourceHeadersOperationBuilder {

    private String mimeType;

    private String filename;

    /**
     * Constructor
     *
     * @param transaction the transaction
     * @param resourceId the fedora identifier
     * @param serverManagedPropsMode server managed properties mode
     */
    public UpdateNonRdfSourceHeadersOperationBuilderImpl(final Transaction transaction,
                                            final FedoraId resourceId,
                                            final ServerManagedPropsMode serverManagedPropsMode) {
        super(transaction, resourceId, serverManagedPropsMode);
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder mimeType(final String mimetype) {
        this.mimeType = mimetype;
        return this;
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder filename(final String filename) {
        this.filename = filename;
        return this;
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder contentDigests(final Collection<URI> digests) {
        throw new UnsupportedOperationException("Not supported for update header operations");
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder contentSize(final long size) {
        throw new UnsupportedOperationException("Not supported for update header operations");
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder userPrincipal(final String userPrincipal) {
        super.userPrincipal(userPrincipal);
        return this;
    }

    @Override
    public UpdateNonRdfSourceHeadersOperationBuilder relaxedProperties(final Model model) {
        super.relaxedProperties(model);
        return this;
    }

    @Override
    public UpdateNonRdfSourceHeadersOperation build() {
        final var operation = new UpdateNonRdfSourceHeadersOperationImpl(transaction, rescId.asBaseId());
        operation.setUserPrincipal(userPrincipal);
        operation.setCreatedBy(createdBy);
        operation.setCreatedDate(createdDate);
        operation.setLastModifiedBy(lastModifiedBy);
        operation.setLastModifiedDate(lastModifiedDate);
        operation.setFilename(filename);
        operation.setMimeType(mimeType);
        return operation;
    }
}
