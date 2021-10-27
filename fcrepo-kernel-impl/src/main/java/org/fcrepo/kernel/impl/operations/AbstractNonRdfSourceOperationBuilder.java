/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;

/**
 * An abstract operation for interacting with a non-rdf source
 *
 * @author bbpennel
 */
public abstract class AbstractNonRdfSourceOperationBuilder implements NonRdfSourceOperationBuilder {

    protected FedoraId resourceId;

    protected InputStream content;

    protected URI externalURI;

    protected String externalType;

    protected String mimeType;

    protected String filename;

    protected Collection<URI> digests;

    protected long contentSize = -1;

    protected String userPrincipal;

    protected Transaction transaction;

    /**
     * Constructor for external binary.
     *
     * @param transaction the transaction
     * @param rescId the internal identifier
     * @param handling the external content handling type.
     * @param externalUri the external content URI.
     */
    protected AbstractNonRdfSourceOperationBuilder(final Transaction transaction, final FedoraId rescId,
                                                   final String handling,
                                                   final URI externalUri) {
        this.transaction = transaction;
        this.resourceId = rescId;
        this.externalURI = externalUri;
        this.externalType = handling;
    }

    /**
     * Constructor for internal binary.
     *
     * @param transaction the transaction
     * @param rescId the internal identifier.
     * @param stream the content stream.
     */
    protected AbstractNonRdfSourceOperationBuilder(final Transaction transaction, final FedoraId rescId,
                                                   final InputStream stream) {
        this.transaction = transaction;
        this.resourceId = rescId;
        this.content = stream;
    }

    @Override
    public NonRdfSourceOperationBuilder mimeType(final String mimetype) {
        this.mimeType = mimetype;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder filename(final String filename) {
        this.filename = filename;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder contentDigests(final Collection<URI> digests) {
        this.digests = digests;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder contentSize(final long size) {
        this.contentSize = size;
        return this;
    }

    @Override
    public NonRdfSourceOperationBuilder userPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }
}
