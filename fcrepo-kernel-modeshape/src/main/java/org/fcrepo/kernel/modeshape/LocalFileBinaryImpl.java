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

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.fcrepo.kernel.api.utils.MessageExternalBodyContentType;
import org.fcrepo.kernel.modeshape.rdf.impl.FixityRdfContext;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;
import org.fcrepo.kernel.modeshape.utils.impl.CacheEntryFactory;
import org.slf4j.Logger;

import com.codahale.metrics.Timer;

/**
 * External binary from a local file
 *
 * @author bbpennel
 * @since 11/29/2017
 */
public class LocalFileBinaryImpl extends FedoraBinaryImpl {
    private static final Logger LOGGER = getLogger(LocalFileBinaryImpl.class);

    /**
     * Constructs a LocalFileBinaryImpl
     *
     * @param node
     */
    public LocalFileBinaryImpl(final Node node) {
        super(node);

        if (node.isNew()) {
            try {
                decorateLocalFileNode(node, new HashSet<>());
            } catch (final RepositoryException e) {
                LOGGER.warn("Count not decorate {} with LocalFileBinaryImpl properties: {}", node, e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.modeshape.FedoraBinaryImpl#getContent()
     */
    @Override
    public InputStream getContent() {
        try {
            return getFileUri().toURL().openStream();
        } catch (final IOException e) {
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
            throws InvalidChecksumException {

        try {
            final Node contentNode = getNode();

            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }

            if (contentType != null) {
                contentNode.setProperty(HAS_MIME_TYPE, contentType);
            }

            if (originalFileName != null) {
                contentNode.setProperty(FILENAME, originalFileName);
            }

            LOGGER.debug("Created content node at path: {}", contentNode.getPath());

            // Ensure provided checksums are valid
            final Collection<URI> nonNullChecksums = (null == checksums) ? new HashSet<>() : checksums;
            verifyChecksums(nonNullChecksums);

            decorateLocalFileNode(contentNode, nonNullChecksums);
            FedoraTypesUtils.touch(getNode());
            FedoraTypesUtils.touch(((FedoraResourceImpl) getDescription()).getNode());

            LOGGER.debug("Set local file content at path: {}", getResourceLocation());

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void verifyChecksums(final Collection<URI> checksums)
            throws InvalidChecksumException, RepositoryException {

        final Property mimeTypeProperty = getProperty(HAS_MIME_TYPE);
        final Map<URI, URI> checksumErrors = new HashMap<>();

        // Loop through provided checksums validating against computed values
        checksums.forEach(checksum -> {
            final String algorithm = ContentDigest.getAlgorithm(checksum);
            try {
                final CacheEntry cacheEntry = CacheEntryFactory.forProperty(mimeTypeProperty);
                cacheEntry.checkFixity(algorithm).stream().findFirst().ifPresent(
                        fixityResult -> {
                            if (!fixityResult.matches(checksum)) {
                                LOGGER.debug("Failed checksum test");
                                checksumErrors.put(checksum, fixityResult.getComputedChecksum());
                            }
                        }
                );
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        });

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
     * @see org.fcrepo.kernel.modeshape.FedoraBinaryImpl#getContentSize()
     */
    @Override
    public long getContentSize() {
        final File file = new File(getFileUri().getPath());
        return file.length();
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

            final Collection<FixityResult> fixityResults
                    = CacheEntryFactory.forProperty(getProperty(HAS_MIME_TYPE)).checkFixity(algorithm);

            return new FixityRdfContext(this, idTranslator, fixityResults, digestUri, contentSize);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.kernel.modeshape.FedoraBinaryImpl#checkFixity(org.fcrepo.kernel.api.identifiers.IdentifierConverter,
     * java.util.Collection)
     */
    @Override
    public Collection<URI> checkFixity(final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Collection<String> algorithms) throws UnsupportedAlgorithmException,
            UnsupportedAccessTypeException {

        fixityCheckCounter.inc();

        return checkFixity(algorithms);
    }

    private Collection<URI> checkFixity(final Collection<String> algorithms) throws UnsupportedAlgorithmException {
        try (final Timer.Context context = timer.time()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking external resource: {}", getResourceLocation());
            }

            return CacheEntryFactory.forProperty(getProperty(HAS_MIME_TYPE)).checkFixity(algorithms);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private String getResourceLocation() {
        try {
            final MessageExternalBodyContentType externalBody
                    = MessageExternalBodyContentType.parse(super.getMimeType());
            return externalBody.getResourceLocation();
        } catch (final UnsupportedAccessTypeException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private URI getFileUri() {
        try {
            return new URI(getResourceLocation());
        } catch (final URISyntaxException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void decorateLocalFileNode(final Node contentNode, final Collection<URI> checksums)
            throws RepositoryException {
        if (contentNode == null) {
            LOGGER.warn("{} node appears to be null!", JCR_CONTENT);
            return;
        }

        final long size = getContentSize();
        contentSizeHistogram.update(size);

        // checksums.add(ContentDigest.asURI(SHA1.algorithm, dsChecksum));

        final String[] checksumArray = new String[checksums.size()];
        checksums.stream().map(Object::toString).collect(Collectors.toSet()).toArray(checksumArray);

        contentNode.setProperty(CONTENT_DIGEST, checksumArray);
        contentNode.setProperty(CONTENT_SIZE, size);

        LOGGER.debug("Decorated data property at path: {}", getResourceLocation());
    }

    /**
     * Returns the specified mimetype in place of the original external-body if provided
     */
    @Override
    public String getMimeType() {
        final String mimeType = super.getMimeType();
        try {
            final MessageExternalBodyContentType extBodyType = MessageExternalBodyContentType.parse(mimeType);
            // Return the overridden mimetype if one is available, otherwise give the original content type
            final String mimeTypeOverride = extBodyType.getMimeType();
            if (mimeTypeOverride == null) {
                return mimeType;
            } else {
                return mimeTypeOverride;
            }
        } catch (final UnsupportedAccessTypeException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
