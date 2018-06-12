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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.slf4j.Logger;

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
     * @see
     * org.fcrepo.kernel.modeshape.FedoraBinaryImpl#setContent(java.io.InputStream, java.lang.String,
     * java.util.Collection, java.lang.String, org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint)
     */
    @Override
    public void setContent(final InputStream content, final String contentType,
                           final Collection<URI> checksums, final String originalFileName,
                           final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Cannot call setContent() on local file, call setExternalContent() instead");
    }

    @Override
    public void setExternalContent(final String contentType,
                                   final Collection<URI> checksums, final String originalFileName,
                                   final String externalHandling, final String externalUrl)
            throws InvalidChecksumException {
        try {
            super.setExternalContent(contentType, checksums, originalFileName, externalHandling, externalUrl);

            final Node descNode = getDescriptionNodeOrNull();
            // Store the size of the file
            final long size = getContentSize();
            contentSizeHistogram.update(size);
            if (descNode != null) {
                descNode.setProperty(CONTENT_SIZE, size);
            }

            LOGGER.debug("Decorated local file data property at path: {}", descNode.getPath());

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public String getMimeType() {
        return getMimeTypeValue();
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
        return getRemoteContentSize();
    }

    @Override
    protected long getRemoteContentSize() {
        final File file = new File(getResourceUri().getPath());
        return file.length();
    }
}
