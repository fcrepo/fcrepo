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

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.api.TripleCategory;
import org.slf4j.Logger;

import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isFedoraBinary;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/19/14
 */
public class FedoraBinaryImpl extends AbstractFedoraBinary {

    private static final Logger LOGGER = getLogger(FedoraBinaryImpl.class);

    private static final String LOCAL_FILE_ACCESS_TYPE = "file";

    private static final String URL_ACCESS_TYPE = "http";

    private FedoraBinary wrappedBinary;

    /**
     * Wrap an existing Node as a Fedora Binary
     * @param node the node
     */
    public FedoraBinaryImpl(final Node node) {
        super(node);
    }

    /**
     * Get the proxied binary content object wrapped by this object
     *
     * @return the fedora binary
     */
    private FedoraBinary getBinary() {
        return getBinary(null);
    }
    /**
     * Get the proxied binary content object wrapped by this object
     *
     * @return the fedora binary
     */
    private FedoraBinary getBinary(final String extUrl) {
        if (wrappedBinary == null) {
            wrappedBinary = getBinaryImplementation(extUrl);
        }
        return wrappedBinary;
    }

    private FedoraBinary getBinaryImplementation(final String extUrl) {
        String url = extUrl;
        if (url == null || url.isEmpty()) {
            url = getURLInfo();
        }


        if (url != null) {
            if (url.toLowerCase().startsWith(LOCAL_FILE_ACCESS_TYPE)) {
                return new LocalFileBinary(getNode());
            } else if (url.toLowerCase().startsWith(URL_ACCESS_TYPE)) {
                return new UrlBinary(getNode());
            }
        }

        return new InternalFedoraBinary(getNode());
    }

    private String getURLInfo() {
        try {
            if (hasProperty(PROXY_FOR)) {
                return getNode().getProperty(PROXY_FOR).getValue().getString();
            } else if (hasProperty(REDIRECTS_TO)) {
                return getNode().getProperty(REDIRECTS_TO).getValue().getString();
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getContent()
     */
    @Override
    public InputStream getContent() {
        return getBinary().getContent();
    }

    @Override
    public void setExternalContent(final String contentType,
                                   final Collection<URI> checksums, final String originalFileName,
                                   final String externalHandling, final String externalUrl)
            throws InvalidChecksumException {

        // Clear the wrapped binary object prior to setting the content
        wrappedBinary = null;
        getBinary(externalUrl).setExternalContent(contentType, checksums, originalFileName,
                externalHandling, externalUrl);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#setContent(java.io.InputStream,
     * java.lang.String, java.net.URI, java.lang.String,
     * org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint)
     */
    @Override
    public void setContent(final InputStream content, final String contentType,
                           final Collection<URI> checksums, final String originalFileName,
                           final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {

        // Clear the wrapped binary object prior to setting the content
        wrappedBinary = null;
        // Need to pass the new filename to get the correct implementation.
        getBinary().setContent(content, contentType, checksums, originalFileName,
                storagePolicyDecisionPoint);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getContentSize()
     */
    @Override
    public long getContentSize() {
        return getBinary().getContentSize();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getContentDigest()
     */
    @Override
    public URI getContentDigest() {
        return getBinary().getContentDigest();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getMimeType()
     */
    @Override
    public String getMimeType() {
        return getBinary().getMimeType();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraBinary#getFilename()
     */
    @Override
    public String getFilename() {
        return getBinary().getFilename();
    }

    @Override
    public RdfStream getFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        return getBinary().getFixity(idTranslator);
    }

    @Override
    public RdfStream getFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                               final URI digestUri,
                               final long size) {

        return getBinary().getFixity(idTranslator, digestUri, size);
    }

    @Override
    public Collection<URI> checkFixity( final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                        final Collection<String> algorithms)
                                            throws UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        return getBinary().checkFixity(idTranslator, algorithms);
    }

    /**
     * When deleting the binary, we also need to clean up the description document.
     */
    @Override
    public void delete() {
        getBinary().delete();
    }

    @Override
    public FedoraResource getBaseVersion() {
        return getBinary().getBaseVersion();
    }

    @Override
    protected boolean hasDescriptionProperty(final String relPath) {
        try {
            final Node descNode = getDescriptionNode();
            if (descNode == null) {
                return false;
            }
            return descNode.hasProperty(relPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    protected Property getDescriptionProperty(final String relPath) {
        try {
            return getDescriptionNode().getProperty(relPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final Set<? extends TripleCategory> contexts) {
        return getDescription().getTriples(idTranslator, contexts);
    }

    /**
     * Check if the given node is a Fedora binary
     *
     * @param node the given node
     * @return whether the given node is a Fedora binary
     */
    public static boolean hasMixin(final Node node) {
        return isFedoraBinary.test(node);
    }
}
