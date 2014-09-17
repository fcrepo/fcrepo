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

import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFrozen;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.resources.BinarySource;
import org.fcrepo.kernel.resources.RdfSource;

import java.io.InputStream;
import java.net.URI;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.exception.ResourceTypeException;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;

/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 *
 * @author ajs6f
 * @since Feb 21, 2013
 */
public class DatastreamImpl extends FedoraResourceImpl implements Datastream, RdfSource<Node> {

    private static final Logger LOGGER = getLogger(DatastreamImpl.class);

    /**
     * The JCR node for this datastream
     *
     * @param n an existing {@link Node}
     * @throws ResourceTypeException if the node existed prior to this call but is not a datastream.
     */
    public DatastreamImpl(final Node n) {
        super(n);

        if (node.isNew()) {
            initializeNewDatastreamProperties();
        } else if (!hasMixin(node) && !isFrozen.apply(n)) {
            throw new ResourceTypeException("Attempting to perform a datastream operation on non-datastream resource!");
        }
    }

    /**
     * Create or find a FedoraDatastream at the given path
     *
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @param nodeType primary type to assign to node
     */
    public DatastreamImpl(final Session session, final String path, final String nodeType) {
        super(session, path, nodeType);
        if (node.isNew()) {
            initializeNewDatastreamProperties();
        } else if (!hasMixin(node) && !isFrozen.apply(node)) {
            throw new ResourceTypeException("Attempting to perform a datastream operation on non-datastream resource!");
        }
    }

    /**
     * Create or find a FedoraDatastream at the given path
     *
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public DatastreamImpl(final Session session, final String path) {
        this(session, path, JcrConstants.NT_FILE);
    }

    private void initializeNewDatastreamProperties() {
        try {
            if (node.isNew() || !hasMixin(node)) {
                LOGGER.debug("Setting {} properties on a {} node...",
                        FEDORA_DATASTREAM, JcrConstants.NT_FILE);
                node.addMixin(FEDORA_DATASTREAM);


                final Node contentNode = findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);
                BinaryImpl.decorateContentNode(contentNode);

            }
        } catch (final RepositoryException ex) {
            LOGGER.warn("Could not decorate {} with {} properties: {}",
                    JCR_CONTENT, FEDORA_DATASTREAM, ex);
        }
    }

    @Override
    public RdfSource<Node> getDescription() {
        return this;
    }

    /**
     * Get the binary source contained by this datastream
     * @return
     */
    public BinarySource<Node,javax.jcr.Binary> getBinary() {
        return new BinaryImpl(this.getContentNode());
    }

    /*
         * (non-Javadoc)
         * @see org.fcrepo.kernel.Datastream#getContent()
         */
    @Override
    public InputStream getContent() {
        return getBinary().getContent();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getBinaryContent()
     */
    @Override
    public javax.jcr.Binary getBinaryContent() {
        return getBinary().getBinaryContent();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getContent()
     */
    @Override
    public Node getContentNode() {
        LOGGER.trace("Retrieved datastream content node.");
        try {
            return node.getNode(JCR_CONTENT);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getContent()
     */
    @Override
    public boolean hasContent() {
        try {
            return node.hasNode(JCR_CONTENT);
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
        getBinary().setContent(content, contentType, checksum, originalFileName, storagePolicyDecisionPoint);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getContentSize()
     */
    @Override
    public long getContentSize() {
        return getBinary().getContentSize();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getContentDigest()
     */
    @Override
    public URI getContentDigest() {
        return getBinary().getContentDigest();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getMimeType()
     */
    @Override
    public String getMimeType() {
        return getBinary().getMimeType();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.Datastream#getFilename()
     */
    @Override
    public String getFilename() {
        return getBinary().getFilename();
    }

    /**
     * Check if the node has a fedora:datastream mixin
     *
     * @param node node to check
     */
    public static boolean hasMixin(final Node node) {
        return isFedoraDatastream.apply(node);
    }

}
