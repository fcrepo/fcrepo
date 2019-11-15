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
package org.fcrepo.persistence.common;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;

import org.fcrepo.persistence.api.ResourceHeaders;


/**
 * Common implementation of resource headers
 *
 * @author bbpennel
 */
public class ResourceHeadersImpl implements ResourceHeaders {

    private String id;

    private String parent;

    private String stateToken;

    private String interactionModel;

    private String mimeType;

    private String filename;

    private long contentSize;

    private Collection<URI> digests;

    private String externalUrl;

    private String externalHandling;

    private Instant createdDate;

    private String createdBy;

    private Instant lastModifiedDate;

    private String lastModifiedBy;

    /**
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @return the parent
     */
    @Override
    public String getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(final String parent) {
        this.parent = parent;
    }

    /**
     * @return the stateToken
     */
    @Override
    public String getStateToken() {
        return stateToken;
    }

    /**
     * @param stateToken the stateToken to set
     */
    public void setStateToken(final String stateToken) {
        this.stateToken = stateToken;
    }

    /**
     * @return the interactionModel
     */
    @Override
    public String getInteractionModel() {
        return interactionModel;
    }

    /**
     * @param interactionModel the interactionModel to set
     */
    public void setInteractionModel(final String interactionModel) {
        this.interactionModel = interactionModel;
    }

    /**
     * @return the mimeType
     */
    @Override
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the filename
     */
    @Override
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * @return the contentSize
     */
    @Override
    public long getContentSize() {
        return contentSize;
    }

    /**
     * @param contentSize the contentSize to set
     */
    public void setContentSize(final long contentSize) {
        this.contentSize = contentSize;
    }

    /**
     * @return the digests
     */
    @Override
    public Collection<URI> getDigests() {
        return digests;
    }

    /**
     * @param digests the digests to set
     */
    public void setDigests(final Collection<URI> digests) {
        this.digests = digests;
    }

    /**
     * @return the externalHandling
     */
    @Override
    public String getExternalHandling() {
        return externalHandling;
    }

    /**
     * @param externalHandling the externalHandling to set
     */
    public void setExternalHandling(final String externalHandling) {
        this.externalHandling = externalHandling;
    }

    /**
     * @return the createdDate
     */
    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(final Instant createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return the createdBy
     */
    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return the lastModifiedDate
     */
    @Override
    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @param lastModifiedDate the lastModifiedDate to set
     */
    public void setLastModifiedDate(final Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedby the lastModifiedby to set
     */
    public void setLastModifiedBy(final String lastModifiedby) {
        this.lastModifiedBy = lastModifiedby;
    }

    /**
     * @param externalUrl the externalUrl to set
     */
    public void setExternalUrl(final String externalUrl) {
        this.externalUrl = externalUrl;
    }

    @Override
    public String getExternalUrl() {
        return externalUrl;
    }
}
