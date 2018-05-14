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
package org.fcrepo.kernel.modeshape;

import static com.codahale.metrics.MetricRegistry.name;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FIELD_DELIMITER;
import static org.fcrepo.kernel.modeshape.services.functions.JcrPropertyFunctions.property2values;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.metrics.RegistryService;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;

/**
 * Abstract class representing the content of a binary resource
 *
 * @author bbpennel
 */
public abstract class AbstractFedoraBinary extends FedoraResourceImpl implements FedoraBinary {

    private static final Logger LOGGER = getLogger(AbstractFedoraBinary.class);

    protected static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    static final RegistryService registryService = RegistryService.getInstance();

    static final Counter fixityCheckCounter = registryService.getMetrics().counter(name(FedoraBinary.class,
            "fixity-check-counter"));

    static final Timer timer = registryService.getMetrics().timer(
            name(NonRdfSourceDescription.class, "fixity-check-time"));

    static final Histogram contentSizeHistogram =
            registryService.getMetrics().histogram(name(FedoraBinary.class, "content-size"));

    protected AbstractFedoraBinary(final Node node) {
        super(node);
    }

    @Override
    public FedoraResource getDescription() {
        final Node descNode = getDescriptionNodeOrNull();
        if (descNode == null) {
            return null;
        }
        return new NonRdfSourceDescriptionImpl(getDescriptionNode());
    }

    @Override
    protected Node getDescriptionNode() {

        try {
            final Node node = getNode();
            if (isMemento()) {
                final String mementoName = node.getName();
                return node.getParent().getParent().getNode(FEDORA_DESCRIPTION)
                        .getNode(LDPCV_TIME_MAP).getNode(mementoName);
            }
            return getNode().getNode(FEDORA_DESCRIPTION);
        } catch (final RepositoryException e) {

            // ignore error as Desc memento may not be there yet
            if (isMemento()) {
                return null;
            }
            throw new RepositoryRuntimeException(e);
        }
        /*
        try {
            return getNode().getNode(FEDORA_DESCRIPTION);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } */
    }

    protected Node getDescriptionNodeOrNull() {
        try {
            return getDescriptionNode();
        } catch (final RepositoryRuntimeException e) {
            if (e.getCause() instanceof PathNotFoundException) {
                return null;
            }
            throw new RepositoryRuntimeException(e);
        }
    }


    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getContentSize()
     */
    @Override
    public long getContentSize() {
        try {
            if (hasDescriptionProperty(CONTENT_SIZE)) {
                return getDescriptionProperty(CONTENT_SIZE).getLong();
            }
        } catch (final RepositoryException e) {
            LOGGER.info("Could not get contentSize(): {}", e.getMessage());
        }

        return -1L;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getContentDigest()
     */
    @Override
    public URI getContentDigest() {

        LOGGER.info("getContentDigest getting digest info");
        try {
            // Determine which digest algorithm to use
            final String algorithm = hasDescriptionProperty(DEFAULT_DIGEST_ALGORITHM) ? property2values.apply(
                getDescriptionProperty(DEFAULT_DIGEST_ALGORITHM)).findFirst().get().getString()
                : ContentDigest.DEFAULT_ALGORITHM;
            final String algorithmWithoutStringType = algorithm.replace(FIELD_DELIMITER + XSDstring.getURI(), "");

            if (hasDescriptionProperty(CONTENT_DIGEST)) {
                // Select the stored digest that matches the digest algorithm
                final Optional<Value> digestValue = property2values.apply(getDescriptionProperty(CONTENT_DIGEST))
                        .filter(digest -> {
                        try {
                            final URI digestUri = URI.create(digest.getString());
                            return algorithmWithoutStringType.equalsIgnoreCase(ContentDigest.getAlgorithm(digestUri));

                        } catch (final RepositoryException e) {
                            LOGGER.warn("Exception thrown when getting digest property {}, {}", digest, e.getMessage());
                            return false;
                        }
                    }).findFirst();

                // Success, return the digest value
                if (digestValue.isPresent()) {
                    return URI.create(digestValue.get().getString());
                }
            }
            LOGGER.warn("No digest value was found to match the algorithm: {}", algorithmWithoutStringType);
        } catch (final RepositoryException e) {
            LOGGER.warn("Could not get content digest: {}", e.getMessage());
        }

        return ContentDigest.missingChecksum();
    }


    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#isProxy()
     */
    @Override
    public Boolean isProxy() {
        return hasProperty(PROXY_FOR);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#isRedirect()
     */
    @Override
    public Boolean isRedirect() {
        return hasProperty(REDIRECTS_TO);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getProxyURL()
     */
    @Override
    public String getProxyURL() {
        try {
            if (hasProperty(PROXY_FOR)) {
                return getProperty(PROXY_FOR).getString();
            }
            return null;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#setProxyURL()
     */
    @Override
    public void setProxyURL(final String url) throws RepositoryRuntimeException {
        try {
            LOGGER.info("Setting Property PROXY_FOR!");
            getNode().setProperty(PROXY_FOR, url);
            getNode().setProperty(REDIRECTS_TO, (Value) null);
        } catch (final Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getRedirectURL()
     */
    @Override
    public String getRedirectURL() {
        try {

            LOGGER.info("get redirect info asking first: {} ", hasProperty(REDIRECTS_TO));
            if (hasProperty(REDIRECTS_TO)) {
                return getProperty(REDIRECTS_TO).getString();
            }

            return null;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#setRedirectURL()
     */
    @Override
    public void setRedirectURL(final String url) throws RepositoryRuntimeException {
        try {
            LOGGER.info("Setting Property REDIRECTS_TO!");
            getNode().setProperty(REDIRECTS_TO, url);
            getNode().setProperty(PROXY_FOR, (Value) null);
        } catch (final Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected String getMimeTypeValue() {
        try {
            if (hasDescriptionProperty(HAS_MIME_TYPE)) {
                return getDescriptionProperty(HAS_MIME_TYPE).getString()
                        .replace(FIELD_DELIMITER + XSDstring.getURI(), "");
            }
        } catch (final RepositoryRuntimeException e) {
            if (!(e.getCause() instanceof PathNotFoundException) || !isMemento()) {
                throw e;
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return DEFAULT_MIME_TYPE;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getFilename()
     */
    @Override
    public String getFilename() {
        try {
            if (hasDescriptionProperty(FILENAME)) {
                return getDescriptionProperty(FILENAME).getString().replace(FIELD_DELIMITER + XSDstring.getURI(), "");
            }
            return node.getName();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public RdfStream getFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        LOGGER.info("getFixity");
        return getFixity(idTranslator, getContentDigest(), getContentSize());
    }

    /**
     * When deleting the binary, we also need to clean up the description document.
     */
    @Override
    public void delete() {
        final FedoraResource description = getDescription();

        if (description != null) {
            description.delete();
        }

        super.delete();
    }

    @Override
    public FedoraResource getBaseVersion() {
        LOGGER.warn("Removed method 'getBaseVersion()' is not used after implementing Memento!");
        return null;
       // return getDescription().getBaseVersion();
    }
/*
    @Override
    public void enableVersioning() {
        super.enableVersioning();
        getDescription().enableVersioning();
    }

    @Override
    public void disableVersioning() {
        super.disableVersioning();
        getDescription().disableVersioning();
    }
*/
    /**
     * Check of the property exists on the description of this binary.
     *
     * @param relPath - path to the property
     * @return true if property exists.
     */
    protected boolean hasDescriptionProperty(final String relPath) {

        try {
            final Node descNode = getDescriptionNodeOrNull();
            if (descNode == null) {
                return false;
            }
            return descNode.hasProperty(relPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Return the description property for this binary.
     *
     * @param relPath - path to the property
     * @return Property object
     */
    protected Property getDescriptionProperty(final String relPath) {
        try {
            return getDescriptionNode().getProperty(relPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Set the content size
     *
     * @param size the new value of the content size.
     */
    protected void setContentSize(final long size) {
        try {
            getDescriptionNode().setProperty(CONTENT_SIZE, size);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
