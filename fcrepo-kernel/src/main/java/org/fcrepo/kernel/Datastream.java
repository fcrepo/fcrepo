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
package org.fcrepo.kernel;

import static com.codahale.metrics.MetricRegistry.name;
import static org.fcrepo.kernel.services.ServiceHelpers.getNodePropertySize;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.ContentDigest;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.ValueFactory;
import org.slf4j.Logger;

import com.codahale.metrics.Histogram;

/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 *
 * @author ajs6f
 * @date Feb 21, 2013
 */
public class Datastream extends FedoraResourceImpl implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(Datastream.class);

    static final Histogram contentSizeHistogram =
            getMetrics().histogram(name(Datastream.class, "content-size"));

    /**
     * The JCR node for this datastream
     * @param n an existing {@link Node}
     */
    public Datastream(final Node n) {
        super(n);

        if (node.isNew()) {
            initializeNewDatastreamProperties();
        }
    }

    /**
     * Create or find a FedoraDatastream at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @param nodeType primary type to assign to node
     * @throws RepositoryException
     */
    public Datastream(final Session session, final String path,
                      final String nodeType) throws RepositoryException {
        super(session, path, nodeType);

        if (node.isNew()) {
            initializeNewDatastreamProperties();
        }
    }

    /**
     * Create or find a FedoraDatastream at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public Datastream(final Session session, final String path)
        throws RepositoryException {
        this(session, path, JcrConstants.NT_FILE);
    }

    private void initializeNewDatastreamProperties() {
        try {
            if (node.isNew() || !hasMixin(node)) {
                LOGGER.debug("Setting {} properties on a {} node...",
                             FEDORA_DATASTREAM, JcrConstants.NT_FILE);
                node.addMixin(FEDORA_DATASTREAM);

                if (node.hasNode(JCR_CONTENT)) {
                    final Node contentNode = node.getNode(JCR_CONTENT);
                    decorateContentNode(contentNode);
                }
            }
        } catch (final RepositoryException ex) {
            LOGGER.warn("Could not decorate {} with {} properties: {}" ,
                        JCR_CONTENT, FEDORA_DATASTREAM, ex);
        }
    }

    /**
     * @return The InputStream of content associated with this datastream.
     * @throws RepositoryException
     */
    public InputStream getContent() throws RepositoryException {
        final Node contentNode = node.getNode(JCR_CONTENT);
        LOGGER.trace("Retrieved datastream content node.");
        return contentNode.getProperty(JCR_DATA).getBinary().getStream();
    }

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
    public void setContent(final InputStream content, final String contentType,
                           final URI checksum, final String originalFileName,
                           final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
        throws RepositoryException, InvalidChecksumException {

        final Node contentNode =
            findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);

        if (contentNode.canAddMixin(FEDORA_BINARY)) {
            contentNode.addMixin(FEDORA_BINARY);
        }

        if (contentType != null) {
            contentNode.setProperty(JCR_MIME_TYPE, contentType);
        }

        if (originalFileName != null) {
            contentNode.setProperty(PREMIS_FILE_NAME, originalFileName);
        }

        LOGGER.debug("Created content node at path: {}", contentNode.getPath());

        String hint = null;


        if (storagePolicyDecisionPoint != null) {
            hint = storagePolicyDecisionPoint.evaluatePolicies(node);
        }
        final ValueFactory modevf =
            (ValueFactory) node.getSession().getValueFactory();
        final Binary binary = modevf.createBinary(content, hint);

        /*
         * This next line of code deserves explanation. If we chose for the
         * simpler line:
         * Property dataProperty = contentNode.setProperty(JCR_DATA,
         * requestBodyStream);
         * then the JCR would not block on the stream's completion, and we would
         * return to the requester before the mutation to the repo had actually
         * completed. So instead we use createBinary(requestBodyStream), because
         * its contract specifies:
         * "The passed InputStream is closed before this method returns either
         * normally or because of an exception."
         * which lets us block and not return until the job is done! The simpler
         * code may still be useful to us for an asynchronous method that we
         * develop later.
         */
        final Property dataProperty = contentNode.setProperty(JCR_DATA, binary);

        final String dsChecksum = binary.getHexHash();
        final URI uriChecksumString = ContentDigest.asURI("SHA-1", dsChecksum);
        if (checksum != null &&
                !checksum.equals(uriChecksumString)) {
            LOGGER.debug("Failed checksum test");
            throw new InvalidChecksumException("Checksum Mismatch of " +
                 uriChecksumString + " and " + checksum);
        }

        decorateContentNode(contentNode);

        LOGGER.debug("Created data property at path: {}",
                     dataProperty.getPath());

    }

    /**
     * Set the content of this datastream
     * @param content Binary content to be stored
     * @throws InvalidChecksumException
     * @throws RepositoryException
     */
    public void setContent(final InputStream content) throws InvalidChecksumException,
                                                       RepositoryException {
        setContent(content, null, null, null, null);
    }

    /**
     * @return The size in bytes of content associated with this datastream.
     */
    public long getContentSize() {
        try {
            return node.getNode(JCR_CONTENT).getProperty(CONTENT_SIZE)
                .getLong();
        } catch (final RepositoryException e) {
            LOGGER.info("Could not get contentSize()", e);
        }

        return -1L;
    }

    /**
     * Get the pre-calculated content digest for the binary payload
     * @return a URI with the format algorithm:value
     * @throws RepositoryException
     */
    public URI getContentDigest() throws RepositoryException {
        final Node contentNode = node.getNode(JCR_CONTENT);
        try {
            return new URI(contentNode.getProperty(CONTENT_DIGEST).getString());
        } catch (final RepositoryException | URISyntaxException e) {
            LOGGER.info("Could not get content digest: ", e);
        }

        return ContentDigest.missingChecksum();
    }

    /**
     * @return The ID of this datastream, unique within an object. Normally just
     *         the name of the backing JCR node.
     * @throws RepositoryException
     */
    public String getDsId() throws RepositoryException {
        return node.getName();
    }

    /**
     * @return the FedoraObject to which this datastream belongs.
     * @throws RepositoryException
     */
    public FedoraObject getObject() throws RepositoryException {
        return new FedoraObject(node.getParent());
    }

    /**
     * @return The MimeType of content associated with this datastream.
     * @throws RepositoryException
     */
    public String getMimeType() throws RepositoryException {
        return node.hasNode(JCR_CONTENT) &&
            node.getNode(JCR_CONTENT).hasProperty(JCR_MIME_TYPE) ?
            node.getNode(JCR_CONTENT).getProperty(JCR_MIME_TYPE).getString() :
            "application/octet-stream";
    }

    /**
     * Return the calculated size of the DS node
     * @return combined size of the properties and binary content
     * @throws RepositoryException
     */
    @Override
    public Long getSize() throws RepositoryException {
        return getNodePropertySize(node) + getContentSize();

    }

    /**
     * Return the file name for the binary content
     * @return original file name for the binary content, or the object's id.
     * @throws RepositoryException
     */
    public String getFilename() throws RepositoryException {
        if (node.hasNode(JCR_CONTENT) && node.getNode(JCR_CONTENT).hasProperty(PREMIS_FILE_NAME)) {
            return node.getNode(JCR_CONTENT).getProperty(PREMIS_FILE_NAME).getString();
        } else {
            return getDsId();
        }
    }

    private void decorateContentNode(final Node contentNode)
        throws RepositoryException {
        if (contentNode == null) {
            LOGGER.warn("{} node appears to be null!", JCR_CONTENT);
            return;
        }
        if (contentNode.canAddMixin(FEDORA_BINARY)) {
            contentNode.addMixin(FEDORA_BINARY);
        }

        final Property dataProperty = contentNode.getProperty(JCR_DATA);
        final Binary binary = (Binary) dataProperty.getBinary();
        final String dsChecksum = binary.getHexHash();

        contentSizeHistogram.update(dataProperty.getLength());

        contentNode.setProperty(CONTENT_SIZE, dataProperty.getLength());
        contentNode.setProperty(CONTENT_DIGEST, ContentDigest.
                                asURI("SHA-1", dsChecksum).toString());

        LOGGER.debug("Decorated data property at path: {}", dataProperty.getPath());
    }

    /**
     * Check if the node has a fedora:datastream mixin
     * @param node node to check
     * @throws RepositoryException
     */
    public static boolean hasMixin(final Node node) throws RepositoryException {
        return isFedoraDatastream.apply(node);
    }

}
