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
package org.fcrepo.kernel.impl;

import com.codahale.metrics.Histogram;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.resources.BinarySource;
import org.fcrepo.kernel.resources.RdfSource;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.metrics.RegistryService;
import org.modeshape.jcr.api.ValueFactory;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static com.codahale.metrics.MetricRegistry.name;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/16/14
 */
public class BinaryImpl extends FedoraResourceImpl implements BinarySource<Node,Binary> {

    private static final Logger LOGGER = getLogger(BinaryImpl.class);

    static final Histogram contentSizeHistogram =
            RegistryService.getInstance().getMetrics().histogram(name(DatastreamImpl.class, "content-size"));

    /**
     * Constructor for an existing jcr:content node
     * @param contentNode
     */
    public BinaryImpl(final Node contentNode) {
        super(contentNode);
    }


    @Override
    public RdfSource<Node> getDescription() {
        try {
            return new FedoraResourceImpl(node.getParent());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public InputStream getContent() {
        try {
            return getBinaryContent().getStream();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Binary getBinaryContent() {
        try {
            return node.getProperty(JCR_DATA).getBinary();
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public long getContentSize() {
        try {
            return node.getProperty(CONTENT_SIZE)
                    .getLong();
        } catch (final RepositoryException e) {
            LOGGER.info("Could not get contentSize(): {}", e.getMessage());
        }

        return -1L;
    }

    @Override
    public URI getContentDigest() {
        try {
            return new URI(node.getProperty(CONTENT_DIGEST).getString());
        } catch (final RepositoryException | URISyntaxException e) {
            LOGGER.info("Could not get content digest: {}", e.getMessage());
        }

        return ContentDigest.missingChecksum();
    }

    @Override
    public String getMimeType() {
        try {
            if (node.hasProperty(JCR_MIME_TYPE)) {
                return node.getProperty(JCR_MIME_TYPE).getString();
            } else {
                return "application/octet-stream";
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public String getFilename() {
        try {
            if (node.hasProperty(PREMIS_FILE_NAME)) {
                return node.getProperty(PREMIS_FILE_NAME).getString();
            }
            return node.getParent().getName();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#setContent(java.io.InputStream,
     * java.lang.String, java.net.URI, java.lang.String,
     * org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint)
     */
    @Override
    public void setContent(final InputStream content, final String contentType,
                           final URI checksum, final String originalFileName,
                           final StoragePolicyDecisionPoint storagePolicyDecisionPoint) {

        try {
            if (node.canAddMixin(FEDORA_BINARY)) {
                node.addMixin(FEDORA_BINARY);
            }

            if (contentType != null) {
                node.setProperty(JCR_MIME_TYPE, contentType);
            }

            if (originalFileName != null) {
                node.setProperty(PREMIS_FILE_NAME, originalFileName);
            }

            LOGGER.debug("Created content node at path: {}", node.getPath());

            String hint = null;

            if (storagePolicyDecisionPoint != null) {
                hint = storagePolicyDecisionPoint.evaluatePolicies(node);
            }
            final ValueFactory modevf =
                    (ValueFactory) node.getSession().getValueFactory();
            final org.modeshape.jcr.api.Binary binary = modevf.createBinary(content, hint);

        /*
         * This next line of code deserves explanation. If we chose for the
         * simpler line: Property dataProperty =
         * contentNode.setProperty(JCR_DATA, requestBodyStream); then the JCR
         * would not block on the stream's completion, and we would return to
         * the requester before the mutation to the repo had actually completed.
         * So instead we use createBinary(requestBodyStream), because its
         * contract specifies: "The passed InputStream is closed before this
         * method returns either normally or because of an exception." which
         * lets us block and not return until the job is done! The simpler code
         * may still be useful to us for an asynchronous method that we develop
         * later.
         */
            final Property dataProperty = node.setProperty(JCR_DATA, binary);

            final String dsChecksum = binary.getHexHash();
            final URI uriChecksumString = ContentDigest.asURI("SHA-1", dsChecksum);
            if (checksum != null &&
                    !checksum.equals(uriChecksumString)) {
                LOGGER.debug("Failed checksum test");
                throw new InvalidChecksumException("Checksum Mismatch of " +
                        uriChecksumString + " and " + checksum);
            }

            decorateContentNode(node);

            LOGGER.debug("Created data property at path: {}", dataProperty.getPath());

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected static void decorateContentNode(final Node contentNode) throws RepositoryException {
        if (contentNode == null) {
            LOGGER.warn("{} node appears to be null!", JCR_CONTENT);
            return;
        }
        if (contentNode.canAddMixin(FEDORA_BINARY)) {
            contentNode.addMixin(FEDORA_BINARY);
        }

        if (contentNode.hasProperty(JCR_DATA)) {
            final Property dataProperty = contentNode.getProperty(JCR_DATA);
            final org.modeshape.jcr.api.Binary binary = (org.modeshape.jcr.api.Binary) dataProperty.getBinary();
            final String dsChecksum = binary.getHexHash();

            contentSizeHistogram.update(dataProperty.getLength());

            contentNode.setProperty(CONTENT_SIZE, dataProperty.getLength());
            contentNode.setProperty(CONTENT_DIGEST, ContentDigest.asURI("SHA-1", dsChecksum).toString());

            LOGGER.debug("Decorated data property at path: {}", dataProperty.getPath());
        }
    }
}
