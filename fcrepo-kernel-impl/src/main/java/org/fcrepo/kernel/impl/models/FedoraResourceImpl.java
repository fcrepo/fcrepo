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

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraID;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.net.URI.create;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;

/**
 * Implementation of a Fedora resource, containing functionality common to the more concrete resource implementations.
 *
 * @author bbpennel
 */
public class FedoraResourceImpl implements FedoraResource {

    private final PersistentStorageSessionManager pSessionManager;

    protected final ResourceFactory resourceFactory;

    private FedoraID fedoraID;

    private String parentId;

    private List<URI> types;

    private Instant lastModifiedDate;

    private String lastModifiedBy;

    private Instant createdDate;

    private String createdBy;

    private Instant mementoDatetime;

    private String stateToken;

    private String etag;

    private boolean isMemento;

    // The transaction this representation of the resource belongs to
    protected final Transaction tx;

    protected FedoraResourceImpl(final FedoraID fedoraID,
                                 final Transaction tx,
                                 final PersistentStorageSessionManager pSessionManager,
                                 final ResourceFactory resourceFactory) {
        this.fedoraID = fedoraID;
        this.tx = tx;
        this.pSessionManager = pSessionManager;
        this.resourceFactory = resourceFactory;
    }

    @Override
    public String getId() {
        return this.fedoraID.getResourceId();
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
        if (isMemento()) {
            try {
                // We are in a memento so we need to create a FedoraID for just the original resource.
                return resourceFactory.getResource(tx, FedoraID.create(getFedoraId().getResourceId()));
            } catch (PathNotFoundException e) {
                throw new PathNotFoundRuntimeException(e);
            }
        }
        return this;
    }

    @Override
    public FedoraResource getTimeMap() {
        if (this.isMemento) {
            return new TimeMapImpl(this.getOriginalResource(), tx, pSessionManager, resourceFactory);
        }
        return new TimeMapImpl(this, tx, pSessionManager, resourceFactory);
    }

    @Override
    public Instant getMementoDatetime() {
        return mementoDatetime;
    }

    @Override
    public boolean isMemento() {
        return isMemento;
    }

    @Override
    public boolean isAcl() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FedoraResource findMementoByDatetime(final Instant mementoDatetime) {
        FedoraResource match = null;
        long matchDiff = 0;

        for (var it = getTimeMap().getChildren().iterator(); it.hasNext();) {
            final var current = it.next();
            // Negative if the memento is AFTER the requested datetime
            // Positive if the memento is BEFORE the requested datetime
            final var diff = Duration.between(current.getMementoDatetime(), mementoDatetime).toSeconds();

            if (match == null                               // Save the first memento examined
                    || (matchDiff < 0 && diff >= matchDiff) // Match is AFTER requested && current is closer
                    || (diff >= 0 && diff <= matchDiff)) {  // Current memento EQUAL/BEFORE request && closer than match
                match = current;
                matchDiff = diff;
            }
        }

        return match;
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
        return getTypes().contains(create(type));
    }

    @Override
    public List<URI> getTypes() {
        if (types == null) {
            types = new ArrayList<>();
            try {
                final var headers = getSession().getHeaders(getId(), getMementoDatetime());
                types.add(create(headers.getInteractionModel()));
                if (headers.isArchivalGroup()) {
                    types.add(create(ARCHIVAL_GROUP.getURI()));
                }
            } catch (final PersistentItemNotFoundException e) {
                throw new ItemNotFoundException("Unable to retrieve headers for " + getId(), e);
            } catch (final PersistentStorageException e) {
                throw new RepositoryRuntimeException(e);
            }

        }

        return types;
    }

    @Override
    public RdfStream getTriples() {
        try {
            final var triples = getSession().getTriples(getId(), getMementoDatetime());

            return new DefaultRdfStream(createURI(getId()), triples);
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
        return !isMemento();
    }

    @Override
    public FedoraResource getDescription() {
        return this;
    }

    @Override
    public FedoraResource getDescribedResource() {
        return this;
    }

    protected PersistentStorageSession getSession() {
        if (tx == null || tx.isCommitted()) {
            return pSessionManager.getReadOnlySession();
        } else {
            return pSessionManager.getSession(tx.getId());
        }
    }

    @Override
    public FedoraResource getParent() throws PathNotFoundException {
        return resourceFactory.getResource(FedoraID.create(parentId));
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public FedoraID getFedoraId() {
        return this.fedoraID;
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

    /**
     * @param isMemento indicates if the resource is a memento
     */
    public void setIsMemento(final boolean isMemento) {
        this.isMemento = isMemento;
    }

    /**
     * Get the Memento Instant as YYYYMMDDHHIISS string.
     * @return The string.
     */
    protected String getMementoDateTimeAsUriString() {
        return MEMENTO_LABEL_FORMATTER.format(getMementoDatetime());
    }
}
