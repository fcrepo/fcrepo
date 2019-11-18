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

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

/**
 * Implementation of a Fedora resource, containing functionality common to the more concrete resource implementations.
 *
 * @author bbpennel
 */
public class FedoraResourceImpl implements FedoraResource {

    private final PersistentStorageSessionManager pSessionManager;

    private final ResourceFactory resourceFactory;

    private final String id;

    private String parentId;

    private List<URI> types;

    private Instant lastModifiedDate;

    private String lastModifiedBy;

    private Instant createdDate;

    private String createdBy;

    private Instant mementoDatetime;

    private String stateToken;

    private String etag;

    // The transaction this representation of the resource belongs to
    private final Transaction tx;

    protected FedoraResourceImpl(final String id,
            final Transaction tx,
            final PersistentStorageSessionManager pSessionManager,
            final ResourceFactory resourceFactory) {
        this.id = id;
        this.tx = tx;
        this.pSessionManager = pSessionManager;
        this.resourceFactory = resourceFactory;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<FedoraResource> getChildren(final Boolean recursive) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FedoraResource getContainer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FedoraResource getOriginalResource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FedoraResource getTimeMap() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Instant getMementoDatetime() {
        return mementoDatetime;
    }

    @Override
    public boolean isMemento() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAcl() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FedoraResource findMementoByDatetime(final Instant mementoDatetime) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FedoraResource getAcl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FedoraResource getChild(final String relPath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasProperty(final String relPath) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public boolean hasType(final String type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<URI> getTypes() {
        return types;
    }

    @Override
    public void addType(final String type) {
        // TODO Auto-generated method stub

    }

    @Override
    public RdfStream getTriples() {
        try {
            return getSession().getTriples(id, getMementoDatetime());
        } catch (final PersistentItemNotFoundException e) {
            throw new ItemNotFoundException("Unable to retrieve triples for " + getId(), e);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Boolean isNew() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getEtagValue() {
        return etag;
    }

    @Override
    public String getStateToken() {
        // TODO Auto-generated method stub
        return stateToken;
    }

    @Override
    public boolean isOriginalResource() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FedoraResource getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FedoraResource getDescribedResource() {
        // TODO Auto-generated method stub
        return null;
    }

    private PersistentStorageSession getSession() {
        return pSessionManager.getSession(tx.getId());
    }

    @Override
    public FedoraResource getParent() throws PathNotFoundException {
        return resourceFactory.getResource(parentId);
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param parentId the parentId to set
     */
    protected void setParentId(final String parentId) {
        this.parentId = parentId;
    }

    /**
     * @param types the types to set
     */
    protected void setTypes(final List<URI> types) {
        this.types = types;
    }

    /**
     * @param lastModifiedDate the lastModifiedDate to set
     */
    protected void setLastModifiedDate(final Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    protected void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @param createdDate the createdDate to set
     */
    protected void setCreatedDate(final Instant createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @param createdBy the createdBy to set
     */
    protected void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @param mementoDatetime the mementoDatetime to set
     */
    protected void setMementoDatetime(final Instant mementoDatetime) {
        this.mementoDatetime = mementoDatetime;
    }

    /**
     * @param stateToken the stateToken to set
     */
    protected void setStateToken(final String stateToken) {
        this.stateToken = stateToken;
    }

    /**
     * @param etag the etag to set
     */
    protected void setEtag(final String etag) {
        this.etag = etag;
    }
}
