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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Adapter for converting between different ResourceHeader implementations.
 *
 * @author pwinckles
 */
public class ResourceHeadersAdapter implements ResourceHeaders {

    private final ResourceHeadersImpl kernelHeaders;
    private final org.fcrepo.storage.ocfl.ResourceHeaders.Builder storageHeaders;

    /**
     * Default constructor
     */
    public ResourceHeadersAdapter() {
        kernelHeaders = new ResourceHeadersImpl();
        storageHeaders = org.fcrepo.storage.ocfl.ResourceHeaders.builder();
    }

    /**
     * @param storageHeaders storage headers to adapt
     */
    public ResourceHeadersAdapter(final org.fcrepo.storage.ocfl.ResourceHeaders storageHeaders) {
        this.storageHeaders = org.fcrepo.storage.ocfl.ResourceHeaders
                .builder(Objects.requireNonNull(storageHeaders, "storageHeaders cannot be null"));
        this.kernelHeaders = new ResourceHeadersImpl();

        if (storageHeaders.getId() != null) {
            kernelHeaders.setId(FedoraId.create(storageHeaders.getId()));
        }
        if (storageHeaders.getParent() != null) {
            kernelHeaders.setParent(FedoraId.create(storageHeaders.getParent()));
        }
        if (storageHeaders.getArchivalGroupId() != null) {
            kernelHeaders.setArchivalGroupId(FedoraId.create(storageHeaders.getArchivalGroupId()));
        }
        kernelHeaders.setArchivalGroup(storageHeaders.isArchivalGroup());
        kernelHeaders.setContentPath(storageHeaders.getContentPath());
        kernelHeaders.setContentSize(storageHeaders.getContentSize());
        kernelHeaders.setCreatedBy(storageHeaders.getCreatedBy());
        kernelHeaders.setCreatedDate(storageHeaders.getCreatedDate());
        kernelHeaders.setDeleted(storageHeaders.isDeleted());
        kernelHeaders.setDigests(new ArrayList<>(storageHeaders.getDigests()));
        this.storageHeaders.withDigests(kernelHeaders.getDigests());
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
        this.storageHeaders = org.fcrepo.storage.ocfl.ResourceHeaders.builder();

        if (kernelHeaders.getId() != null) {
            storageHeaders.withId(kernelHeaders.getId().getFullId());
        }
        if (kernelHeaders.getParent() != null) {
            storageHeaders.withParent(kernelHeaders.getParent().getFullId());
        }
        if (kernelHeaders.getArchivalGroupId() != null) {
            storageHeaders.withArchivalGroupId(kernelHeaders.getArchivalGroupId().getFullId());
        }
        storageHeaders.withArchivalGroup(kernelHeaders.isArchivalGroup());
        storageHeaders.withContentPath(kernelHeaders.getContentPath());
        storageHeaders.withContentSize(kernelHeaders.getContentSize());
        storageHeaders.withCreatedBy(kernelHeaders.getCreatedBy());
        storageHeaders.withCreatedDate(kernelHeaders.getCreatedDate());
        storageHeaders.withDeleted(kernelHeaders.isDeleted());
        storageHeaders.withDigests(kernelHeaders.getDigests());
        storageHeaders.withExternalHandling(kernelHeaders.getExternalHandling());
        storageHeaders.withExternalUrl(kernelHeaders.getExternalUrl());
        storageHeaders.withFilename(kernelHeaders.getFilename());
        storageHeaders.withInteractionModel(kernelHeaders.getInteractionModel());
        storageHeaders.withLastModifiedBy(kernelHeaders.getLastModifiedBy());
        storageHeaders.withLastModifiedDate(kernelHeaders.getLastModifiedDate());
        storageHeaders.withMimeType(kernelHeaders.getMimeType());
        storageHeaders.withObjectRoot(kernelHeaders.isObjectRoot());
        storageHeaders.withStateToken(kernelHeaders.getStateToken());
    }

    /**
     * @return the headers as storage headers
     */
    public org.fcrepo.storage.ocfl.ResourceHeaders asStorageHeaders() {
        return storageHeaders.build();
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
        storageHeaders.withId(idToString(id));
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
        storageHeaders.withParent(idToString(parent));
    }

    @Override
    public FedoraId getArchivalGroupId() {
        return kernelHeaders.getArchivalGroupId();
    }

    /**
     * @param archivalGroupId the archivalGroupId to set
     */
    public void setArchivalGroupId(final FedoraId archivalGroupId) {
        kernelHeaders.setArchivalGroupId(archivalGroupId);
        storageHeaders.withArchivalGroupId(idToString(archivalGroupId));
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
        storageHeaders.withStateToken(stateToken);
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
        storageHeaders.withInteractionModel(interactionModel);
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
        storageHeaders.withMimeType(mimeType);
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
        storageHeaders.withFilename(filename);
    }

    @Override
    public long getContentSize() {
        return kernelHeaders.getContentSize();
    }

    /**
     * @param contentSize the contentSize to set
     */
    public void setContentSize(final Long contentSize) {
        kernelHeaders.setContentSize(contentSize);
        storageHeaders.withContentSize(contentSize);
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
        storageHeaders.withDigests(digests);
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
        storageHeaders.withExternalHandling(externalHandling);
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
        storageHeaders.withCreatedDate(createdDate);
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
        storageHeaders.withCreatedBy(createdBy);
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
        storageHeaders.withLastModifiedDate(lastModifiedDate);
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
        storageHeaders.withLastModifiedBy(lastModifiedBy);
    }

    /**
     * @param externalUrl the externalUrl to set
     */
    public void setExternalUrl(final String externalUrl) {
        kernelHeaders.setExternalUrl(externalUrl);
        storageHeaders.withExternalUrl(externalUrl);
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
        storageHeaders.withArchivalGroup(flag);
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
        storageHeaders.withObjectRoot(flag);
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
        storageHeaders.withDeleted(deleted);
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
        storageHeaders.withContentPath(contentPath);
    }

}
