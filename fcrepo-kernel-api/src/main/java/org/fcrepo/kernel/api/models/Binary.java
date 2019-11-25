/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.api.models;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;

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
     * Set the content stream for this resource.
     *
     * @param content Inputstream
     */
    @Deprecated
    void setContentStream(InputStream content);

    /**
     * Sets the content of this Datastream.
     *
     * @param content  InputStream of binary content to be stored
     * @param contentType MIME type of content (optional)
     * @param checksums Collection of checksum URIs of the content (optional)
     * @param originalFileName Original file name of the content (optional)
     * @param storagePolicyDecisionPoint Policy decision point for storing the content (optional)
     * @throws InvalidChecksumException if invalid checksum exception occurred
     */
    @Deprecated
    void setContent(InputStream content, String contentType, Collection<URI> checksums,
                    String originalFileName,
                    StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException;

    /**
     * Sets the external content reference for this datastream
     *
     * @param contentType MIME type of content (optional)
     * @param checksums Collection of checksum URIs of the content (optional)
     * @param originalFileName Original file name of the content (optional)
     * @param externalHandling What type of handling the external resource needs (proxy or redirect)
     * @param externalUrl Url for the external resourcej
     * @throws InvalidChecksumException if invalid checksum exception occurred
     */
    @Deprecated
    void setExternalContent(String contentType, Collection<URI> checksums,
                    String originalFileName, String externalHandling, String externalUrl)
            throws InvalidChecksumException;
    /**
     * @return The size in bytes of content associated with this datastream.
     */
    long getContentSize();

    /**
     * Get the pre-calculated content digest for the binary payload
     * @return a URI with the format algorithm:value
     */
    URI getContentDigest();

    /**
     * @return Whether or not this binary is a proxy to another resource
     */
    Boolean isProxy();

    /**
     * @return Whether or not this binary is a redirect to another resource
     */
    Boolean isRedirect();

    /**
     * Get the URL that this resource is a Proxy for
     * @return String containing URL of object to proxy
     */
    String getProxyURL();

    /**
     * Set the URL that this resource is a proxy for
     * @param url - the url of the resource this is a proxy for
     */
    @Deprecated
    void setProxyURL(String url);

    /**
     * Get the URL this resource should redirect to
     * @return String containing URL of object to redirect to
     */
    String getRedirectURL();

    /**
     * Get URL as a URI
     *
     * @return URI containing the object to redirect to.
     */
    default URI getRedirectURI() {
        return URI.create(getRedirectURL());
    }

    /**
     * Set the URL that this is a redirect to
     *
     * @param url - the url of the resource this redirects to
     */
    @Deprecated
    void setRedirectURL(String url);

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
