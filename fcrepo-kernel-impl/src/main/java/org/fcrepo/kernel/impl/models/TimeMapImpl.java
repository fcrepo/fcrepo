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

import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
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

/**
 * FedoraResource implementation that represents a Memento TimeMap of the base resource.
 */
public class TimeMapImpl extends FedoraResourceImpl implements TimeMap {

    private static final List<URI> TYPES = List.of(
            URI.create(RdfLexicon.TIME_MAP.getURI()),
            URI.create(RdfLexicon.VERSIONING_TIMEMAP.getURI())
    );

    private final FedoraResource originalResource;
    private List<Instant> versions;

    protected TimeMapImpl(
            final FedoraResource originalResource,
            final Transaction tx,
            final PersistentStorageSessionManager pSessionManager,
            final ResourceFactory resourceFactory) {
        super(originalResource.getFedoraId().addToFullId(FCR_VERSIONS), tx, pSessionManager, resourceFactory);

        this.originalResource = originalResource;
        setCreatedBy(originalResource.getCreatedBy());
        setCreatedDate(originalResource.getCreatedDate());
        setLastModifiedBy(originalResource.getLastModifiedBy());
        setLastModifiedDate(originalResource.getLastModifiedDate());
        setParentId(originalResource.getId());
        setEtag(originalResource.getEtagValue());
        setStateToken(originalResource.getStateToken());
        setTypes(TYPES);
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
    public Stream<FedoraResource> getChildren(final Boolean recursive) {
        return getVersions().stream().map(version -> {
            try {
                return resourceFactory.getResource(tx, getInstantFedoraId(version));
            } catch (final PathNotFoundException e) {
                throw new PathNotFoundRuntimeException(e);
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
    public FedoraResource getTimeMap() {
        return this;
    }

    private List<Instant> getVersions() {
        if (versions == null) {
            try {
                versions = getSession().listVersions(getId());
            } catch (final PersistentItemNotFoundException e) {
                throw new ItemNotFoundException("Unable to retrieve versions for " + getId(), e);
            } catch (final PersistentStorageException e) {
                throw new RepositoryRuntimeException(e);
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
        final String versionTime = MEMENTO_LABEL_FORMATTER.format(version);
        return getFedoraId().addToFullId(versionTime);
    }

}
