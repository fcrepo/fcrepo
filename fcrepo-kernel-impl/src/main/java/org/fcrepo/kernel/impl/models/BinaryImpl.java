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
package org.fcrepo.kernel.impl.models;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;


/**
 * Implementation of a Non-RDF resource.
 *
 * @author bbpennel
 */
public class BinaryImpl extends FedoraResourceImpl implements Binary {

    private String externalHandling;

    private String externalUrl;

    private Long contentSize;

    private String filename;

    private String mimeType;

    private Collection<URI> digests;

    /**
     * Construct the binary
     *
     * @param id fedora identifier
     * @param tx transaction
     * @param pSessionManager session manager
     * @param resourceFactory resource factory
     */
    public BinaryImpl(final String id, final Transaction tx, final PersistentStorageSessionManager pSessionManager,
            final ResourceFactory resourceFactory) {
        super(id, tx, pSessionManager, resourceFactory);
    }

    @Override
    public InputStream getContent() {
        try {
            return getSession().getBinaryContent(getId(), getMementoDatetime());
        } catch (final PersistentItemNotFoundException e) {
            throw new ItemNotFoundException("Unable to find content for " + getId()
                    + " version " + getMementoDatetime(), e);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void setContent(final InputStream content, final String contentType, final Collection<URI> checksums,
            final String originalFileName, final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {
        // TODO Auto-generated method stub

    }

    @Override
    public long getContentSize() {
        return contentSize;
    }

    @Override
    public URI getContentDigest() {
        // Returning the first digest for the time being
        if (digests == null) {
            return null;
        }
        final var digest = digests.stream().findFirst();
        return digest.isPresent() ? digest.get() : null;
    }

    @Override
    public Boolean isProxy() {
        return PROXY.equals(externalHandling);
    }

    @Override
    public Boolean isRedirect() {
        return ExternalContent.REDIRECT.equals(externalHandling);
    }

    @Override
    public String getExternalURL() {
        return externalUrl;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public FedoraResource getDescription() {
        try {
            final var descId = getId() + "/" + FCR_METADATA;
            return resourceFactory.getResource(tx, descId);
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        }
    }

    /**
     * @param externalHandling the externalHandling to set
     */
    protected void setExternalHandling(final String externalHandling) {
        this.externalHandling = externalHandling;
    }

    /**
     * @param externalUrl the externalUrl to set
     */
    protected void setExternalUrl(final String externalUrl) {
        this.externalUrl = externalUrl;
    }

    /**
     * @param contentSize the contentSize to set
     */
    protected void setContentSize(final Long contentSize) {
        this.contentSize = contentSize;
    }

    /**
     * @param filename the filename to set
     */
    protected void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * @param mimeType the mimeType to set
     */
    protected void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @param digests the digests to set
     */
    protected void setDigests(final Collection<URI> digests) {
        this.digests = digests;
    }
}
