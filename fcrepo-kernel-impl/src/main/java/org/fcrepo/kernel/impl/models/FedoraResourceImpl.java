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
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;

/**
 * Implementation of a Fedora resource, containing functionality common to the more concrete resource implementations.
 *
 * @author bbpennel
 */
public class FedoraResourceImpl implements FedoraResource {

    private PersistentStorageSessionManager pSessionManager;

    private String id;

    private ResourceHeaders headers;

    // The transaction this representation of the resource belongs to
    private Transaction tx;

    protected FedoraResourceImpl(final ResourceHeaders headers, final Transaction tx,
            final PersistentStorageSessionManager pSessionManager) {
        this.id = headers.getId();
        this.headers = headers;
        this.tx = tx;
        this.pSessionManager = pSessionManager;
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
    public ResourceHeaders getHeaders() {
        return headers;
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
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Instant getLastModifiedDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasType(final String type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<URI> getTypes() {
        // TODO Auto-generated method stub
        return null;
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
        }
    }

    @Override
    public RdfStream getManagedProperties() {
        try {
            return getSession().getManagedProperties(id, getMementoDatetime());
        } catch (final PersistentItemNotFoundException e) {
            throw new ItemNotFoundException("Unable to retrieve managed properties for " + getId(), e);
        }
    }

    @Override
    public Boolean isNew() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getEtagValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getStateToken() {
        // TODO Auto-generated method stub
        return null;
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
}
