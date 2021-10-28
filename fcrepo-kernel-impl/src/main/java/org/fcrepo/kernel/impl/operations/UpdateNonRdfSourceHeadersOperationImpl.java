/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.apache.commons.lang3.NotImplementedException;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.UpdateNonRdfSourceHeadersOperation;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

/**
 * Operation to update the headers of a non-rdf resource
 * @author bbpennel
 */
public class UpdateNonRdfSourceHeadersOperationImpl extends AbstractRelaxableResourceOperation
                                                    implements UpdateNonRdfSourceHeadersOperation {

    private String mimeType;

    private String filename;

    public UpdateNonRdfSourceHeadersOperationImpl(final Transaction transaction, final FedoraId resourceId) {
        super(transaction, resourceId);
    }

    @Override
    public InputStream getContentStream() {
        throw new UnsupportedOperationException("Not supported for header update operation");
    }

    @Override
    public String getExternalHandling() {
        throw new UnsupportedOperationException("Not supported for header update operation");
    }

    @Override
    public URI getContentUri() {
        throw new UnsupportedOperationException("Not supported for header update operation");
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public Collection<URI> getContentDigests() {
        throw new NotImplementedException("Not supported for header update operation");
    }

    @Override
    public long getContentSize() {
        throw new NotImplementedException("Not supported for header update operation");
    }

    /**
     * @param mimeType the mimeType to set
     */
    protected void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @param filename the filename to set
     */
    protected void setFilename(final String filename) {
        this.filename = filename;
    }
}
