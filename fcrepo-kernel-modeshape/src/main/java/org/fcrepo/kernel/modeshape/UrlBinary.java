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

//import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.api.FedoraExternalContent.PROXY;
import static org.fcrepo.kernel.api.FedoraExternalContent.REDIRECT;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.api.utils.CacheEntry;
import org.fcrepo.kernel.api.utils.ContentDigest;
import org.fcrepo.kernel.api.utils.FixityResult;
import org.fcrepo.kernel.modeshape.rdf.impl.FixityRdfContext;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;
import org.fcrepo.kernel.modeshape.utils.impl.CacheEntryFactory;
import org.slf4j.Logger;

import com.codahale.metrics.Timer;

/**
 * External binary from a url
 *
 * @author bbpennel
 * @since 12/14/2017
 */
public class UrlBinary extends AbstractFedoraBinary {
    private static final Logger LOGGER = getLogger(UrlBinary.class);

    /**
     * Construct UrlBinary
     *
     * @param node node
     */
    public UrlBinary(final Node node) {
        super(node);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.modeshape.FedoraBinaryImpl#getContent()
     */
    @Override
    public InputStream getContent() {
        // todo - this needs to be more complete so the proxy information will
        // make it up to the higher levels. Ie, so one can pass back the response information
        try {
            return getResourceUri().toURL().openStream();
        } catch (final IOException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected long getRemoteContentSize() {
        final URI resourceUri = getResourceUri();
        try {
            final HttpURLConnection httpConn = (HttpURLConnection) resourceUri.toURL().openConnection();
            httpConn.setRequestMethod("HEAD");
            httpConn.connect();

            final int status = httpConn.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                final String contentLength = httpConn.getHeaderField("Content-Length");
                if (contentLength != null) {
                    try {
                        return Long.parseLong(contentLength);
                    } catch (final NumberFormatException e) {
                        LOGGER.warn("Unable to parse Content-Length of remote file {}", resourceUri, e);
                    }
                }
            }

        } catch (final IOException e) {
            LOGGER.warn("Error getting content size for '{}' : '{}'", getPath(), resourceUri, e);
        }
        return -1L;
    }

    @Override
    public void setExternalContent(final String contentType,
                           final Collection<URI> checksums, final String originalFileName,
                           final String externalHandling, final String externalUrl)
            throws InvalidChecksumException {

        // set a few things on the description node, then set a few more in the other setContent() function
        final Node descNode = getDescriptionNodeOrNull();
        final Node contentNode = getNode();
        try {
            if (externalHandling.equals(PROXY)) {
                contentNode.setProperty(PROXY_FOR, externalUrl);
            } else if (externalHandling.equals(REDIRECT)) {
                contentNode.setProperty(REDIRECTS_TO, externalUrl);
            } else {
                throw new RepositoryException("Unknown external content handling type: " + externalHandling);
            }

            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }

            LOGGER.debug("Created content node at path: {}", contentNode.getPath());

            // Ensure provided checksums are valid
            final Collection<URI> nonNullChecksums = (null == checksums) ? new HashSet<>() : checksums;
            verifyChecksums(nonNullChecksums);

            // Store checksums on node
            final String[] checksumArray = new String[nonNullChecksums.size()];
            nonNullChecksums.stream().map(Object::toString).collect(Collectors.toSet()).toArray(checksumArray);

            FedoraTypesUtils.touch(contentNode);

            if (descNode != null) {
                descNode.setProperty(CONTENT_DIGEST, checksumArray);
                if (contentType != null) {
                    descNode.setProperty(HAS_MIME_TYPE, contentType);
                } else {
                    descNode.setProperty(HAS_MIME_TYPE, DEFAULT_MIME_TYPE);
                }

                if (originalFileName != null) {
                    descNode.setProperty(FILENAME, originalFileName);
                }
                setContentSize(getRemoteContentSize());

                FedoraTypesUtils.touch(descNode);
            }

            LOGGER.debug("Set url binary content from path: {}", getResourceLocation());

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.modeshape.FedoraBinaryImpl#setContent(java.io.InputStream, java.lang.String,
     * java.util.Collection, java.lang.String, org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint)
     */
    @Override
    public void setContent(final InputStream content, final String contentType,
            final Collection<URI> checksums, final String originalFileName,
            final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Cannot call setContent() on external binary. Call setExternalContent() instead.");
    }


    protected void verifyChecksums(final Collection<URI> checksums)
            throws InvalidChecksumException, RepositoryException {

        Property property = null;
        if (isProxy()) {
            property = getProperty(PROXY_FOR);
        } else if (isRedirect()) {
            property = getProperty(REDIRECTS_TO);
        } // what else could it be?

        final Map<URI, URI> checksumErrors = new HashMap<>();

        final CacheEntry cacheEntry = CacheEntryFactory.forProperty(property);
        // Loop through provided checksums validating against computed values
        for (final URI check : checksums) {
            final String algorithm = ContentDigest.getAlgorithm(check);
            for (final FixityResult result : cacheEntry.checkFixity(algorithm)) {
                if (!result.matches(check)) {
                    LOGGER.debug("Failed checksum test");
                    checksumErrors.put(check, result.getComputedChecksum());
                }
            }
        }

        // Throw an exception if any checksum errors occurred
        if (!checksumErrors.isEmpty()) {
            final String template = "Checksum Mismatch of %1$s and %2$s\n";
            final StringBuilder error = new StringBuilder();
            checksumErrors.forEach((key, value) -> error.append(String.format(template, key, value)));
            throw new InvalidChecksumException(error.toString());
        }

    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.modeshape.FedoraBinaryImpl#getFixity(org.fcrepo.kernel.api.identifiers.IdentifierConverter)
     */
    @Override
    public RdfStream getFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        return getFixity(idTranslator, getContentDigest(), getContentSize());
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.modeshape.FedoraBinaryImpl#getFixity(org.fcrepo.kernel.api.identifiers.IdentifierConverter,
     * java.net.URI, long)
     */
    @Override
    public RdfStream getFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator, final URI digestUri,
            final long size) {

        fixityCheckCounter.inc();

        try (final Timer.Context context = timer.time()) {

            LOGGER.debug("Checking resource: " + getPath());

            final String algorithm = ContentDigest.getAlgorithm(digestUri);

            final long contentSize = size < 0 ? getContentSize() : size;

            Collection<FixityResult> fixityResults = null;
            if (isProxy()) {
                fixityResults = CacheEntryFactory.forProperty(getProperty(PROXY_FOR)).checkFixity(algorithm);
            } else if (isRedirect()) {
                fixityResults =
                    CacheEntryFactory.forProperty(getProperty(REDIRECTS_TO)).checkFixity(algorithm);
            } else {
                LOGGER.warn("URL Binary -- not proxy or redirect, so what is it?");
                throw new RepositoryException("Binary resource marked as external is not proxy or redirect.");
            }
            return new FixityRdfContext(this, idTranslator, fixityResults, digestUri, contentSize);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Collection<URI> checkFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Collection<String> algorithms)
            throws UnsupportedAlgorithmException, UnsupportedAccessTypeException {

        fixityCheckCounter.inc();

        try (final Timer.Context context = timer.time()) {

            Collection<URI> list = null;
            if (isProxy()) {
                list = CacheEntryFactory.forProperty(getProperty(PROXY_FOR)).checkFixity(algorithms);
            } else if (isRedirect()) {
                list = CacheEntryFactory.forProperty(getProperty(REDIRECTS_TO)).checkFixity(algorithms);
            } else {
                throw new RepositoryRuntimeException("External content error: not proxy or redirect");
            }
            return list;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

    }

    /**
     * Returns the specified mimetype in place of the original external-body if provided
     */
    @Override
    public String getMimeType() {
        return getMimeTypeValue();
    }

    /**
     * Gets the URL for where this resource is actually located
     * @return String containing actual location of resource
     */
    protected String getResourceLocation() {
        if (isProxy()) {
            return getProxyURL();
        } else {
            return getRedirectURL();
        }
    }

    /**
     * Get a URI for the resource
     * @return URI object representing resource's location
     */
    protected URI getResourceUri() {
        try {
            return new URI(getResourceLocation());
        } catch (final URISyntaxException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
