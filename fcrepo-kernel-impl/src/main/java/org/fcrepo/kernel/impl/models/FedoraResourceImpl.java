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
import org.fcrepo.persistence.api.PersistentStorageSessionFactory;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;

/**
 * Implementation of a Fedora resource, containing functionality common to the more concrete resource implementations.
 *
 * @author bbpennel
 */
public class FedoraResourceImpl implements FedoraResource {
    private PersistentStorageSessionFactory pSessionFactory;

    private String id;

    // The transaction this representation of the resource belongs to
    private Transaction tx;

    private RdfStream triplesStream;

    private RdfStream managedPropertiesStream;

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
    public FedoraResource findOrCreateAcl() {
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
    public void delete() {
        // TODO Auto-generated method stub

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
        if (triplesStream == null) {
            try {
                pSessionFactory.getSession(tx.getId())
                        .readTriples(this);
            } catch (final PersistentItemNotFoundException e) {
                throw new ItemNotFoundException("Unable to retrieve triples for " + getId(), e);
            }
        }

        return triplesStream;
    }

    @Override
    public void setTriples(final RdfStream triplesStream) {
        this.triplesStream = triplesStream;
    }

    @Override
    public RdfStream getManagedProperties() {
        if (managedPropertiesStream == null) {
            try {
                pSessionFactory.getSession(tx.getId())
                        .readManagedProperties(this);
            } catch (final PersistentItemNotFoundException e) {
                throw new ItemNotFoundException("Unable to retrieve triples for " + getId(), e);
            }
        }

        return managedPropertiesStream;
    }

    @Override
    public void setManagedProperties(final RdfStream managedStream) {
        this.managedPropertiesStream = managedStream;
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

}
