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

import org.apache.jena.graph.Triple;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
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
import static java.util.stream.Collectors.toList;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEGATE_TYPE;

/**
 * Implementation of a Fedora resource, containing functionality common to the more concrete resource implementations.
 *
 * @author bbpennel
 */
public class FedoraResourceImpl implements FedoraResource {

    private final PersistentStorageSessionManager pSessionManager;

    protected final ResourceFactory resourceFactory;

    protected final FedoraId fedoraId;

    private FedoraId parentId;

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
    protected final String txId;

    protected FedoraResourceImpl(final FedoraId fedoraId,
                                 final String txId,
                                 final PersistentStorageSessionManager pSessionManager,
                                 final ResourceFactory resourceFactory) {
        this.fedoraId = fedoraId;
        this.txId = txId;
        this.pSessionManager = pSessionManager;
        this.resourceFactory = resourceFactory;
    }

    @Override
    public String getId() {
        return this.fedoraId.getResourceId();
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<FedoraResource> getChildren(final Boolean recursive) {
        return Stream.empty();
    }

    @Override
    public FedoraResource getContainer() {
        return resourceFactory.getContainer(txId, fedoraId);
    }

    @Override
    public FedoraResource getOriginalResource() {
        if (isMemento()) {
            try {
                // We are in a memento so we need to create a FedoraId for just the original resource.
                final var fedoraId = FedoraId.create(getFedoraId().getResourceId());
                return getFedoraResource(fedoraId);
            } catch (final PathNotFoundException e) {
                throw new PathNotFoundRuntimeException(e);
            }
        }
        return this;
    }

    private FedoraResource getFedoraResource(final FedoraId fedoraId) throws PathNotFoundException {
        return resourceFactory.getResource(txId, fedoraId);
    }

    @Override
    public FedoraResource getTimeMap() {
        return new TimeMapImpl(this.getOriginalResource(), txId, pSessionManager, resourceFactory);
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
        return false;
    }

    @Override
    public FedoraResource findMementoByDatetime(final Instant mementoDatetime) {
        FedoraResource match = null;
        long matchDiff = 0;

        for (final var it = getTimeMap().getChildren().iterator(); it.hasNext();) {
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
        if (isAcl()) {
            return this;
        }
        try {
            final var aclId = fedoraId.asAcl();
            return getFedoraResource(aclId);
        } catch (final PathNotFoundException e) {
            return null;
        }
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
            types.addAll(getSystemTypes(false));
            types.addAll(getUserTypes());
        }
        return types;
    }

    @Override
    public List<URI> getSystemTypes(final boolean forRdf) {
        try {
            final List<URI> types = new ArrayList<>();
            final var headers = getSession().getHeaders(getFedoraId().asResourceId(), getMementoDatetime());
            types.add(create(headers.getInteractionModel()));
            // ldp:Resource is on all resources
            types.add(create(RESOURCE.toString()));
            types.add(create(FEDORA_RESOURCE.getURI()));
            if (!forRdf) {
                // These types are not exposed as RDF triples.
                if (headers.isArchivalGroup()) {
                    types.add(create(ARCHIVAL_GROUP.getURI()));
                }
                if (isMemento) {
                    types.add(create(MEMENTO_TYPE));
                } else if (isOriginalResource()) {
                    types.addAll(List.of(create(VERSIONED_RESOURCE.getURI()), create(VERSIONING_TIMEGATE_TYPE)));
                }
            }
            return types;
        } catch (final PersistentItemNotFoundException e) {
            throw new ItemNotFoundException("Unable to retrieve headers for " + getId(), e);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public List<URI> getUserTypes() {
        try {
            final var description = getDescription();
            final var triples = getSession().getTriples(description.getFedoraId().asResourceId(),
                    description.getMementoDatetime());
            return triples.filter(t -> t.predicateMatches(type.asNode())).map(Triple::getObject)
                    .map(t -> URI.create(t.toString())).collect(toList());
        } catch (final PersistentItemNotFoundException e) {
            throw new ItemNotFoundException("Unable to retrieve triples for " + getId(), e);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public RdfStream getTriples() {
        try {
            final var subject = createURI(getId());
            final var triples = getSession().getTriples(getFedoraId().asResourceId(), getMementoDatetime());

            return new DefaultRdfStream(subject, triples);
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
        if (txId == null) {
            return pSessionManager.getReadOnlySession();
        } else {
            return pSessionManager.getSession(txId);
        }
    }

    @Override
    public FedoraResource getParent() throws PathNotFoundException {
        return resourceFactory.getResource(txId, parentId);
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
    public FedoraId getFedoraId() {
        return this.fedoraId;
    }

    /**
     * @param parentId the parentId to set
     */
    protected void setParentId(final FedoraId parentId) {
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

}
