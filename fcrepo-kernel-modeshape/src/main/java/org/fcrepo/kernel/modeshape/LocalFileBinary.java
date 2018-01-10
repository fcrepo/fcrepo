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

import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.api.utils.MessageExternalBodyContentType;
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
public class LocalFileBinary extends UrlBinary {
    private static final Logger LOGGER = getLogger(LocalFileBinary.class);

    /**
     * Constructs a LocalFileBinaryImpl
     *
     * @param node node
     */
    public LocalFileBinary(final Node node) {
        super(node);
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

        if (contentType == null) {
            throw new IllegalArgumentException(
                    "ContentType must be non-null when setting content for local file binary");
        }

        try {
            final Node contentNode = getNode();

            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }

            if (originalFileName != null) {
                contentNode.setProperty(FILENAME, originalFileName);
            }

            contentNode.setProperty(HAS_MIME_TYPE, contentType);

            // Store the required jcr:data property
            contentNode.setProperty(JCR_DATA, "");

            LOGGER.debug("Created content node at path: {}", contentNode.getPath());

            // Ensure provided checksums are valid
            final Collection<URI> nonNullChecksums = (null == checksums) ? new HashSet<>() : checksums;
            verifyChecksums(nonNullChecksums);

            // Store checksums on node
            final String[] checksumArray = new String[nonNullChecksums.size()];
            nonNullChecksums.stream().map(Object::toString).collect(Collectors.toSet()).toArray(checksumArray);
            contentNode.setProperty(CONTENT_DIGEST, checksumArray);

            // Store the size of the file
            final long size = getContentSize();
            contentSizeHistogram.update(size);
            contentNode.setProperty(CONTENT_SIZE, size);

            FedoraTypesUtils.touch(getNode());
            FedoraTypesUtils.touch(((FedoraResourceImpl) getDescription()).getNode());

            LOGGER.debug("Set local file content at path: {}", getResourceLocation());

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public String getMimeType() {
        final String mimeType = getMimeTypeValue();

        try {
            final MessageExternalBodyContentType extBodyType = MessageExternalBodyContentType.parse(mimeType);
            // Return the overridden mimetype if one is available, otherwise give generic binary mimetype
            final String mimeTypeOverride = extBodyType.getMimeType();
            if (mimeTypeOverride == null) {
                return "application/octet-stream";
            } else {
                return mimeTypeOverride;
            }
        } catch (final UnsupportedAccessTypeException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.modeshape.FedoraBinaryImpl#getContentSize()
     */
    @Override
    public long getContentSize() {
        final long sizeValue = super.getContentSize();
        if (sizeValue > -1L) {
            return sizeValue;
        }
        final File file = new File(getResourceUri().getPath());
        return file.length();
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
}
