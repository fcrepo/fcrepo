/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.api.models;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;

import javax.jcr.Binary;
import java.io.InputStream;
import java.net.URI;

/**
 * @author cabeer
 * @since 9/19/14
 */
public interface FedoraBinary extends NonRdfSource {

    /**
     * @return The InputStream of content associated with this datastream.
     */
    InputStream getContent();


    /**
     * @return The Binary content associated with this datastream.
     */
    Binary getBinaryContent();

    /**
     * Sets the content of this Datastream.
     *
     * @param content  InputStream of binary content to be stored
     * @param contentType MIME type of content (optional)
     * @param checksum Checksum URI of the content (optional)
     * @param originalFileName Original file name of the content (optional)
     * @param storagePolicyDecisionPoint Policy decision point for storing the content (optional)
     * @throws org.fcrepo.kernel.api.exception.InvalidChecksumException if invalid checksum exception occurred
     */
    void setContent(InputStream content, String contentType, URI checksum,
                    String originalFileName,
                    StoragePolicyDecisionPoint storagePolicyDecisionPoint)
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
     * @return The MimeType of content associated with this datastream.
     */
    String getMimeType();

    /**
     * Return the file name for the binary content
     * @return original file name for the binary content, or the object's id.
     */
    String getFilename();

    /**
     * Get the fixity of this datastream compared to metadata stored in the repository
     * @param idTranslator the id translator
     * @return the fixity of this datastream compared to metadata stored in the repository
     */
    RdfStream getFixity(IdentifierConverter<Resource, FedoraResource> idTranslator);

    /**
     * Get the fixity of this datastream in a given repository's binary store.
     * @param idTranslator the id translator
     * @param contentDigest the checksum to compare against
     * @param size the expected size of the binary
     * @return the fixity of the datastream
     */
    RdfStream getFixity(IdentifierConverter<Resource, FedoraResource> idTranslator,
                        URI contentDigest, long size);
}
