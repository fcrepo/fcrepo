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
package org.fcrepo.kernel;

import java.io.InputStream;
import java.net.URI;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;

/**
 * @author bbpennel
 * @since Feb 18, 2014
 */
public interface Datastream extends FedoraResource {

    /**
     * @return The InputStream of content associated with this datastream.
     * @throws RepositoryException
     */
    InputStream getContent() throws RepositoryException;

    /**
     * @return The Binary content associated with this datastream.
     * @throws RepositoryException
     */
    Binary getBinaryContent() throws RepositoryException;

    /**
     * @return The Node of content associated with this datastream.
     * @throws RepositoryException
     */
    Node getContentNode() throws RepositoryException;

    /**
     * Sets the content of this Datastream.
     *
     * @param content  InputStream of binary content to be stored
     * @param contentType MIME type of content (optional)
     * @param checksum Checksum URI of the content (optional)
     * @param originalFileName Original file name of the content (optional)
     * @param storagePolicyDecisionPoint Policy decision point for storing the content (optional)
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    void setContent(InputStream content, String contentType, URI checksum,
            String originalFileName,
            StoragePolicyDecisionPoint storagePolicyDecisionPoint)
        throws RepositoryException, InvalidChecksumException;

    /**
     * @return The size in bytes of content associated with this datastream.
     */
    long getContentSize();

    /**
     * Get the pre-calculated content digest for the binary payload
     * @return a URI with the format algorithm:value
     * @throws RepositoryException
     */
    URI getContentDigest() throws RepositoryException;

    /**
     * @return The ID of this datastream, unique within an object. Normally just
     *         the name of the backing JCR node.
     * @throws RepositoryException
     */
    String getDsId() throws RepositoryException;

    /**
     * @return the FedoraObject to which this datastream belongs.
     * @throws RepositoryException
     */
    FedoraObject getObject() throws RepositoryException;

    /**
     * @return The MimeType of content associated with this datastream.
     * @throws RepositoryException
     */
    String getMimeType() throws RepositoryException;

    /**
     * Return the calculated size of the DS node
     * @return combined size of the properties and binary content
     * @throws RepositoryException
     */
    @Override
    Long getSize() throws RepositoryException;

    /**
     * Return the file name for the binary content
     * @return original file name for the binary content, or the object's id.
     * @throws RepositoryException
     */
    String getFilename() throws RepositoryException;

}