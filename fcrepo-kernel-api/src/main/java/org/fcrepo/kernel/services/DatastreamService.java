/**
 * Copyright 2013 DuraSpace, Inc.
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
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.iterators.RdfStream;

/**
 * @author bbpennel
 * @date Feb 20, 2014
 */
public interface DatastreamService extends Service {

    /**
     * Create a stub datastream without content
     * @param session
     * @param dsPath
     * @return
     * @throws RepositoryException
     */
    Datastream createDatastream(Session session, String dsPath) throws RepositoryException;

    /**
     * Create a new Datastream node in the repository
     *
     * @param session
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param requestBodyStream binary payload for the datastream
     * @return
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    Node createDatastreamNode(Session session, String dsPath, String contentType, String originalFileName,
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
    Node createDatastreamNode(Session session, String dsPath, String contentType, String originalFileName,
            InputStream requestBodyStream, URI checksum) throws RepositoryException, InvalidChecksumException;

    /**
     * Retrieve the JCR node for a Datastream by pid and dsid
     *
     * @param path
     * @return
     * @throws RepositoryException
     */
    Node getDatastreamNode(Session session, String path) throws RepositoryException;

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param path jcr path to the datastream
     * @return
     * @throws RepositoryException
     */
    Datastream getDatastream(Session session, String path) throws RepositoryException;

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return
     */
    Datastream asDatastream(Node node);

    /**
     * Get the fixity results for the datastream as a RDF Dataset
     *
     * @param subjects
     * @param datastream
     * @return
     * @throws RepositoryException
     */
    RdfStream getFixityResultsModel(GraphSubjects subjects, Datastream datastream) throws RepositoryException;

    /**
     * Run the fixity check on the datastream and attempt to automatically
     * correct failures if additional copies of the bitstream are available
     *
     * @param datastream
     * @return
     * @throws RepositoryException
     */
    Collection<FixityResult> runFixityAndFixProblems(Datastream datastream) throws RepositoryException;

    /**
     * Get the fixity results for this datastream's bitstream, and compare it
     * against the given checksum and size.
     *
     * @param resource
     * @param dsChecksum -the checksum and algorithm represented as a URI
     * @param dsSize
     * @return
     * @throws RepositoryException
     */
    Collection<FixityResult> getFixity(Node resource, URI dsChecksum, long dsSize) throws RepositoryException;

}