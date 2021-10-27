/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.models;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

/**
 * @author cabeer
 * @since 9/19/14
 */
public interface Binary extends FedoraResource {

    /**
     * @return The InputStream of content associated with this datastream.
     */
    InputStream getContent();

    /**
     * @return The size in bytes of content associated with this datastream.
     */
    long getContentSize();

    /**
     * Get the pre-calculated content digest for the binary payload
     * @return a URI with the format algorithm:value
     */
    Collection<URI> getContentDigests();

    /**
     * @return Whether or not this binary is a proxy to another resource
     */
    Boolean isProxy();

    /**
     * @return Whether or not this binary is a redirect to another resource
     */
    Boolean isRedirect();

    /**
     * @return the external url for this binary if present, or null.
     */
    String getExternalURL();

    /**
     * @return Get the external uri for this binary if present, or null
     */
    default URI getExternalURI() {
        final var externalUrl = getExternalURL();
        if (externalUrl == null) {
            return null;
        }
        return URI.create(externalUrl);
    }

    /**
     * @return The MimeType of content associated with this datastream.
     */
    String getMimeType();

    /**
     * Return the file name for the binary content
     * @return original file name for the binary content, or the object's id.
     */
    String getFilename();
}
