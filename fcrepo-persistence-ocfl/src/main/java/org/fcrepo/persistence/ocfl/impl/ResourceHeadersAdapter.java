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

package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.common.ResourceHeadersImpl;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

/**
 * Adapter for converting between different ResourceHeader implementations.
 *
 * @author pwinckles
 */
public class ResourceHeadersAdapter implements ResourceHeaders {

    private final ResourceHeadersImpl kernelHeaders;
    private final org.fcrepo.storage.ocfl.ResourceHeaders storageHeaders;

    /**
     * Default constructor
     */
    public ResourceHeadersAdapter() {
        kernelHeaders = new ResourceHeadersImpl();
        storageHeaders = new org.fcrepo.storage.ocfl.ResourceHeaders();
    }

    /**
     * @param storageHeaders storage headers to adapt
     */
    public ResourceHeadersAdapter(final org.fcrepo.storage.ocfl.ResourceHeaders storageHeaders) {
        this.storageHeaders = Objects.requireNonNull(storageHeaders, "storageHeaders cannot be null");
        this.kernelHeaders = new ResourceHeadersImpl();

        if (storageHeaders.getId() != null) {
            kernelHeaders.setId(FedoraId.create(storageHeaders.getId()));
        }
        if (storageHeaders.getParent() != null) {
            kernelHeaders.setParent(FedoraId.create(storageHeaders.getParent()));
        }
        kernelHeaders.setArchivalGroup(storageHeaders.isArchivalGroup());
        kernelHeaders.setContentPath(storageHeaders.getContentPath());
        kernelHeaders.setContentSize(storageHeaders.getContentSize());
        kernelHeaders.setCreatedBy(storageHeaders.getCreatedBy());
        kernelHeaders.setCreatedDate(storageHeaders.getCreatedDate());
        kernelHeaders.setDeleted(storageHeaders.isDeleted());
        kernelHeaders.setDigests(storageHeaders.getDigests());
        kernelHeaders.setExternalHandling(storageHeaders.getExternalHandling());
        kernelHeaders.setExternalUrl(storageHeaders.getExternalUrl());
        kernelHeaders.setFilename(storageHeaders.getFilename());
        kernelHeaders.setInteractionModel(storageHeaders.getInteractionModel());
        kernelHeaders.setLastModifiedBy(storageHeaders.getLastModifiedBy());
        kernelHeaders.setLastModifiedDate(storageHeaders.getLastModifiedDate());
        kernelHeaders.setMimeType(storageHeaders.getMimeType());
        kernelHeaders.setObjectRoot(storageHeaders.isObjectRoot());
        kernelHeaders.setStateToken(storageHeaders.getStateToken());
    }

    /**
     * @param kernelHeaders kernel headers to adapt
     */
    public ResourceHeadersAdapter(final ResourceHeadersImpl kernelHeaders) {
        this.kernelHeaders = Objects.requireNonNull(kernelHeaders, "kernelHeaders cannot be null");
        this.storageHeaders = new org.fcrepo.storage.ocfl.ResourceHeaders();

        if (kernelHeaders.getId() != null) {
            storageHeaders.setId(kernelHeaders.getId().getFullId());
        }
        if (kernelHeaders.getParent() != null) {
            storageHeaders.setParent(kernelHeaders.getParent().getFullId());
        }
        storageHeaders.setArchivalGroup(kernelHeaders.isArchivalGroup());
        storageHeaders.setContentPath(kernelHeaders.getContentPath());
        storageHeaders.setContentSize(kernelHeaders.getContentSize());
        storageHeaders.setCreatedBy(kernelHeaders.getCreatedBy());
        storageHeaders.setCreatedDate(kernelHeaders.getCreatedDate());
        storageHeaders.setDeleted(kernelHeaders.isDeleted());
        storageHeaders.setDigests(kernelHeaders.getDigests());
        storageHeaders.setExternalHandling(kernelHeaders.getExternalHandling());
        storageHeaders.setExternalUrl(kernelHeaders.getExternalUrl());
        storageHeaders.setFilename(kernelHeaders.getFilename());
        storageHeaders.setInteractionModel(kernelHeaders.getInteractionModel());
        storageHeaders.setLastModifiedBy(kernelHeaders.getLastModifiedBy());
        storageHeaders.setLastModifiedDate(kernelHeaders.getLastModifiedDate());
        storageHeaders.setMimeType(kernelHeaders.getMimeType());
        storageHeaders.setObjectRoot(kernelHeaders.isObjectRoot());
        storageHeaders.setStateToken(kernelHeaders.getStateToken());
    }

    /**
     * @return the headers as storage headers
     */
    public org.fcrepo.storage.ocfl.ResourceHeaders asStorageHeaders() {
        return storageHeaders;
    }

    /**
     * @return the headers as kernel headers
     */
    public ResourceHeadersImpl asKernelHeaders() {
        return kernelHeaders;
    }

    private String idToString(final FedoraId id) {
        if (id == null) {
            return null;
        }
        return id.getFullId();
    }

    @Override
    public FedoraId getId() {
        return kernelHeaders.getId();
    }

    /**
     * @param id the fedora id to set
     */
    public void setId(final FedoraId id) {
        kernelHeaders.setId(id);
        storageHeaders.setId(idToString(id));
    }

    @Override
    public FedoraId getParent() {
        return kernelHeaders.getParent();
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(final FedoraId parent) {
        kernelHeaders.setParent(parent);
        storageHeaders.setParent(idToString(parent));
    }

    @Override
    public String getStateToken() {
        return kernelHeaders.getStateToken();
    }

    /**
     * @param stateToken the stateToken to set
     */
    public void setStateToken(final String stateToken) {
        kernelHeaders.setStateToken(stateToken);
        storageHeaders.setStateToken(stateToken);
    }

    @Override
    public String getInteractionModel() {
        return kernelHeaders.getInteractionModel();
    }

    /**
     * @param interactionModel the interactionModel to set
     */
    public void setInteractionModel(final String interactionModel) {
        kernelHeaders.setInteractionModel(interactionModel);
        storageHeaders.setInteractionModel(interactionModel);
    }

    @Override
    public String getMimeType() {
        return kernelHeaders.getMimeType();
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(final String mimeType) {
        kernelHeaders.setMimeType(mimeType);
        storageHeaders.setMimeType(mimeType);
    }

    @Override
    public String getFilename() {
        return kernelHeaders.getFilename();
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(final String filename) {
        kernelHeaders.setFilename(filename);
        storageHeaders.setFilename(filename);
    }

    @Override
    public Long getContentSize() {
        return kernelHeaders.getContentSize();
    }

    /**
     * @param contentSize the contentSize to set
     */
    public void setContentSize(final Long contentSize) {
        kernelHeaders.setContentSize(contentSize);
        storageHeaders.setContentSize(contentSize);
    }

    @Override
    public Collection<URI> getDigests() {
        return kernelHeaders.getDigests();
    }

    /**
     * @param digests the digests to set
     */
    public void setDigests(final Collection<URI> digests) {
        kernelHeaders.setDigests(digests);
        storageHeaders.setDigests(digests);
    }

    @Override
    public String getExternalHandling() {
        return kernelHeaders.getExternalHandling();
    }

    /**
     * @param externalHandling the externalHandling to set
     */
    public void setExternalHandling(final String externalHandling) {
        kernelHeaders.setExternalHandling(externalHandling);
        storageHeaders.setExternalHandling(externalHandling);
    }

    @Override
    public Instant getCreatedDate() {
        return kernelHeaders.getCreatedDate();
    }

    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(final Instant createdDate) {
        kernelHeaders.setCreatedDate(createdDate);
        storageHeaders.setCreatedDate(createdDate);
    }

    @Override
    public String getCreatedBy() {
        return kernelHeaders.getCreatedBy();
    }

    /**
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(final String createdBy) {
        kernelHeaders.setCreatedBy(createdBy);
        storageHeaders.setCreatedBy(createdBy);
    }

    @Override
    public Instant getLastModifiedDate() {
        return kernelHeaders.getLastModifiedDate();
    }

    /**
     * @param lastModifiedDate the lastModifiedDate to set
     */
    public void setLastModifiedDate(final Instant lastModifiedDate) {
        kernelHeaders.setLastModifiedDate(lastModifiedDate);
        storageHeaders.setLastModifiedDate(lastModifiedDate);
    }

    @Override
    public String getLastModifiedBy() {
        return kernelHeaders.getLastModifiedBy();
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    public void setLastModifiedBy(final String lastModifiedBy) {
        kernelHeaders.setLastModifiedBy(lastModifiedBy);
        storageHeaders.setLastModifiedBy(lastModifiedBy);
    }

    /**
     * @param externalUrl the externalUrl to set
     */
    public void setExternalUrl(final String externalUrl) {
        kernelHeaders.setExternalUrl(externalUrl);
        storageHeaders.setExternalUrl(externalUrl);
    }

    @Override
    public String getExternalUrl() {
        return kernelHeaders.getExternalUrl();
    }

    /**
     *
     * @param flag boolean flag
     */
    public void setArchivalGroup(final boolean flag) {
        kernelHeaders.setArchivalGroup(flag);
        storageHeaders.setArchivalGroup(flag);
    }

    @Override
    public boolean isArchivalGroup() {
        return kernelHeaders.isArchivalGroup();
    }

    /**
     * @param flag boolean flag
     */
    public void setObjectRoot(final boolean flag) {
        kernelHeaders.setObjectRoot(flag);
        storageHeaders.setObjectRoot(flag);
    }

    @Override
    public boolean isObjectRoot() {
        if (isArchivalGroup()) {
            return true;
        } else {
            return kernelHeaders.isObjectRoot();
        }
    }

    /**
     * Set deleted status flag.
     * @param deleted true if deleted (a tombstone).
     */
    public void setDeleted(final boolean deleted) {
        kernelHeaders.setDeleted(deleted);
        storageHeaders.setDeleted(deleted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeleted() {
        return kernelHeaders.isDeleted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentPath() {
        return kernelHeaders.getContentPath();
    }

    /**
     * Sets the path to the content file associated with the header file
     *
     * @param contentPath path to content file
     */
    public void setContentPath(final String contentPath) {
        kernelHeaders.setContentPath(contentPath);
        storageHeaders.setContentPath(contentPath);
    }

}
