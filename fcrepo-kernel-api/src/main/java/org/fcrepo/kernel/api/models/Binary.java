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

import java.io.InputStream;
import java.net.URI;

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
