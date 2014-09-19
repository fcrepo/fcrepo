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
package org.fcrepo.kernel.services;

import java.io.InputStream;
import java.net.URI;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.ResourceTypeException;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;

/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface DatastreamService extends Service {

    /**
     * Create a new Datastream node in the repository
     * 
     * @param session
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param originalFileName the original file name for the input stream
     * @param requestBodyStream binary payload for the datastream
     * @return created datastream
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    Datastream createDatastream(Session session, String dsPath, String contentType, String originalFileName,
            InputStream requestBodyStream) throws RepositoryException, InvalidChecksumException;

    /**
     * Create a new Datastream node in the repository
     *
     * @param session
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param originalFileName the original file name for the input stream
     * @param requestBodyStream binary payload for the datastream
     * @param checksum the digest for the binary payload (as urn:sha1:xyz) @return
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    Datastream createDatastream(Session session, String dsPath, String contentType, String originalFileName,
            InputStream requestBodyStream, URI checksum) throws InvalidChecksumException;

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param path jcr path to the datastream
     * @return retrieved Datastream
     * @throws RepositoryException
     */
    Datastream getDatastream(Session session, String path);

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return node as a Datastream
     */
    Datastream asDatastream(Node node) throws ResourceTypeException;

    /**
     * Get the fixity results for the datastream as a RDF Dataset
     *
     * @param subjects
     * @param datastream
     * @return fixity results for datastream
     * @throws RepositoryException
     */
    RdfStream getFixityResultsModel(IdentifierTranslator subjects, Datastream datastream);

}