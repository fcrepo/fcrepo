/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.models;

import java.io.InputStream;
import java.net.URI;

/**
 * Interface for the ExternalContent information class.
 * @author bseeger
 */
public interface ExternalContent {

    String PROXY = "proxy";
    String REDIRECT = "redirect";
    String COPY = "copy";

    /**
     * Returns the content type located in the link header.
     * @return content type if in Link header, else null
     */
    public String getContentType();

    /**
     * Returns the size of the content located at the link header
     * @return content size
     */
    public long getContentSize();

    /**
     * Retrieve handling information
     * @return a String containing the type of handling requested ["proxy", "copy" or "redirect"]
     */
    public String getHandling();

    /**
     * Retrieve url in link header
     * @return a String of the URL that was in the Link header
     */
    public String getURL();

    /**
     * Retrieve URI in link header
     * @return a URI to the external content
     */
    public URI getURI();

    /**
     * Returns whether or not the handling parameter is "copy"
     * @return boolean value representing whether or not the content handling is "copy"
     */
    public boolean isCopy();

    /**
     * Returns whether or not the handling parameter is "redirect"
     * @return boolean value representing whether or not the content handling is "redirect"
     */
    public boolean isRedirect();

    /**
     * Returns whether or not the handling parameter is "proxy"
     * @return boolean value representing whether or not the content handling is "proxy"
     */
    public boolean isProxy();

    /**
     * Fetch the external content
     * @return InputStream containing the external content
     */
    public InputStream fetchExternalContent();
}
