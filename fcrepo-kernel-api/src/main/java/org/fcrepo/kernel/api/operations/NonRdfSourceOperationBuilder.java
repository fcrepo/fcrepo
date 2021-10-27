/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import java.net.URI;
import java.util.Collection;

/**
 * Builder for an operation for interacting with a non-rdf source
 *
 * @author bbpennel
 */
public interface NonRdfSourceOperationBuilder extends ResourceOperationBuilder {

    /**
     * Set the mimetype for content in this resource
     *
     * @param mimetype the mime-type.
     * @return the builder.
     */
    NonRdfSourceOperationBuilder mimeType(String mimetype);

    /**
     * Set the filename
     *
     * @param filename name of the file.
     * @return the builder.
     */
    NonRdfSourceOperationBuilder filename(String filename);

    /**
     * Collection of digests for content in this resource
     *
     * @param digests collection of digests
     * @return the builder.
     */
    NonRdfSourceOperationBuilder contentDigests(Collection<URI> digests);

    /**
     * Set the number of bytes for the content
     *
     * @param size size of the content in bytes
     * @return the builder
     */
    NonRdfSourceOperationBuilder contentSize(long size);

    @Override
    NonRdfSourceOperationBuilder userPrincipal(String userPrincipal);

    @Override
    NonRdfSourceOperation build();
}
