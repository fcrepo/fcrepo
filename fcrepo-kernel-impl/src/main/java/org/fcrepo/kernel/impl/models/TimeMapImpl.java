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

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.URI.create;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;

/**
 * FedoraResource implementation that represents a Memento TimeMap of the base resource.
 *
 * @author pwinckles
 */
public class TimeMapImpl extends FedoraResourceImpl implements TimeMap {

    /**
     * Types of this class that should be displayed
     */
    private static final List<URI> HEADER_AND_RDF_TYPES = List.of(
            create(RESOURCE.getURI()),
            create(CONTAINER.getURI()),
            create(RDF_SOURCE.getURI())
    );

    /**
     * Above types but also types not to be displayed in RDF bodies.
     */
    private static final List<URI> HEADER_ONLY_TYPES = Stream.concat(HEADER_AND_RDF_TYPES.stream(),
            List.of(
                create(RdfLexicon.VERSIONING_TIMEMAP.getURI())
            ).stream()
    ).collect(Collectors.toList());

    private final FedoraResource originalResource;
    private List<Instant> versions;

    protected TimeMapImpl(
            final FedoraResource originalResource,
            final String txId,
            final PersistentStorageSessionManager pSessionManager,
            final ResourceFactory resourceFactory) {
        super(originalResource.getFedoraId().asTimemap(), txId, pSessionManager, resourceFactory);

        this.originalResource = originalResource;
        setCreatedBy(originalResource.getCreatedBy());
        setCreatedDate(originalResource.getCreatedDate());
        setLastModifiedBy(originalResource.getLastModifiedBy());
        setLastModifiedDate(originalResource.getLastModifiedDate());
        setParentId(originalResource.getFedoraId().asResourceId());
        setEtag(originalResource.getEtagValue());
        setStateToken(originalResource.getStateToken());
    }

    @Override
    public RdfStream getTriples() {
        final var timeMapResource = asResource(this);
        final var model = ModelFactory.createDefaultModel();
        model.add(new StatementImpl(timeMapResource, RdfLexicon.MEMENTO_ORIGINAL_RESOURCE,
                asResource(getOriginalResource())));
        getChildren().map(this::asResource).forEach(child -> {
            model.add(new StatementImpl(timeMapResource, RdfLexicon.CONTAINS, child));
        });
        return DefaultRdfStream.fromModel(timeMapResource.asNode(), model);
    }

    @Override
    public List<URI> getSystemTypes(final boolean forRdf) {
        // TimeMaps don't have an on-disk representation so don't call super.getSystemTypes().
        if (forRdf) {
            return HEADER_AND_RDF_TYPES;
        }
        return HEADER_ONLY_TYPES;
    }

    @Override
    public List<URI> getUserTypes() {
        // TimeMaps don't have user triples.
        return Collections.emptyList();
    }

    @Override
    public Stream<FedoraResource> getChildren(final Boolean recursive) {
        return getVersions().stream().map(version -> {
            try {
                final var fedoraId = getInstantFedoraId(version);
                return resourceFactory.getResource(txId, fedoraId);
            } catch (final PathNotFoundException e) {
                throw new PathNotFoundRuntimeException(e.getMessage(), e);
            }
        });
    }

    @Override
    public FedoraResource getOriginalResource() {
        return originalResource;
    }

    @Override
    public boolean isOriginalResource() {
        return false;
    }

    @Override
    public TimeMap getTimeMap() {
        return this;
    }

    private List<Instant> getVersions() {
        if (versions == null) {
            try {
                versions = getSession().listVersions(getFedoraId().asResourceId());
            } catch (final PersistentItemNotFoundException e) {
                throw new ItemNotFoundException("Unable to retrieve versions for " + getId(), e);
            } catch (final PersistentStorageException e) {
                throw new RepositoryRuntimeException(e.getMessage(), e);
            }
        }
        return versions;
    }

    private Resource asResource(final FedoraResource fedoraResource) {
        return org.apache.jena.rdf.model.ResourceFactory.createResource(fedoraResource.getFedoraId().getFullId());
    }

    /**
     * Get a FedoraId for a memento with the specified version datetime.
     * @param version The instant datetime.
     * @return the new FedoraId for the current TimeMap and the version.
     */
    private FedoraId getInstantFedoraId(final Instant version) {
        return getFedoraId().asMemento(version);
    }

    @Override
    public List<Instant> listMementoDatetimes() {
        return getVersions();
    }

}
