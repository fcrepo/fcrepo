/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

/**
 * An operation for interacting with a non-RDF source resource.
 *
 * @author bbpennel
 */
public interface NonRdfSourceOperation extends ResourceOperation {

    /**
     * @return the content stream for a local binary
     */
    InputStream getContentStream();

    /**
     * @return the handling method for external content in this resource
     */
    String getExternalHandling();

    /**
     * @return the URI for external content in this resource
     */
    URI getContentUri();

    /**
     * @return The MimeType of content associated with this resource.
     */
    String getMimeType();

    /**
     * Return the file name for the binary content
     *
     * @return original file name for the binary content, or the object's id.
     */
    String getFilename();

    /**
     * @return the URIs of digests for the content in this resource
     */
    Collection<URI> getContentDigests();

    /**
     * @return The size in bytes of content associated with this resource.
     */
    long getContentSize();
}
