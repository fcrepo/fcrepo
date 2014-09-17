/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.resources;

import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;

import java.io.InputStream;
import java.net.URI;

/**
 * @author cabeer
 * @since 9/16/14
 */
public interface BinarySource<T, V> extends Resource<T> {
    /**
     * Get the description document for this resource
     * @return
     */
    RdfSource<T> getDescription();

    /**
     * @return The InputStream of content associated with this datastream.
     */
    InputStream getContent();

    /**
     * @return The Binary content associated with this datastream.
     */
    V getBinaryContent();

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
     * @return The MimeType of content associated with this datastream.
     */
    String getMimeType();

    /**
     * Return the file name for the binary content
     * @return original file name for the binary content, or the object's id.
     */
    String getFilename();

    /**
     * Add binary content
     * @param content
     * @param contentType
     * @param checksum
     * @param originalFileName
     * @param storagePolicyDecisionPoint
     */
    void setContent(final InputStream content, final String contentType,
                    final URI checksum, final String originalFileName,
                    final StoragePolicyDecisionPoint storagePolicyDecisionPoint);
}
