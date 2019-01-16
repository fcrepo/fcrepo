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
package org.fcrepo.kernel.modeshape.services;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.FedoraExternalContent.PROXY;
import static org.fcrepo.kernel.api.FedoraExternalContent.REDIRECT;
import static org.fcrepo.kernel.api.FedoraTypes.CONTENT_DIGEST;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.FedoraTypes.MEMENTO;
import static org.fcrepo.kernel.api.FedoraTypes.MEMENTO_DATETIME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_CONTAINMENT;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_MEMBERSHIP;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;
import static org.fcrepo.kernel.api.RdfLexicon.LDPCV_TIME_MAP;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.rdf.impl.RequiredPropertiesUtil.assertRequiredContainerTriples;
import static org.fcrepo.kernel.modeshape.rdf.impl.RequiredPropertiesUtil.assertRequiredDescriptionTriples;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.api.utils.SubjectMappingUtil.mapSubject;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.modeshape.ContainerImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.InternalIdentifierTranslator;
import org.fcrepo.kernel.modeshape.utils.iterators.RelaxedRdfAdder;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;

/**
 * This service exposes management of node versioning for resources and binaries.
 *
 * @author Mike Durbin
 * @author bbpennel
 */

@Component
public class VersionServiceImpl extends AbstractService implements VersionService {

    private static final Logger LOGGER = getLogger(VersionService.class);

    @VisibleForTesting
    public static final Set<TripleCategory> VERSION_TRIPLES = new HashSet<>(asList(
            PROPERTIES, SERVER_MANAGED, LDP_MEMBERSHIP, LDP_CONTAINMENT));

    /**
     * The bitstream service
     */
    @Inject
    private BinaryService binaryService;

    @Inject
    private RdfNamespaceRegistry namespaceRegistry;

    @Override
    public FedoraResource createVersion(final FedoraSession session, final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator, final Instant dateTime) {
        return createVersion(session, resource, idTranslator, dateTime, null, null);
    }

    @Override
    public FedoraResource createVersion(final FedoraSession session,
            final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Instant dateTime,
            final InputStream rdfInputStream,
            final Lang rdfFormat) {

        final String mementoPath = makeMementoPath(resource, dateTime);
        assertMementoDoesNotExist(session, mementoPath);

        // Construct an unpopulated resource of the appropriate type for new memento
        final FedoraResource mementoResource;
        if (resource instanceof Container) {
            mementoResource = createContainer(session, mementoPath);
        } else {
            mementoResource = binaryService.findOrCreateDescription(session, mementoPath);
        }

        final String mementoUri = getUri(mementoResource, idTranslator);
        final String resourceUri = getUri(resource.getDescribedResource(), idTranslator);

        final RdfStream mementoRdfStream;
        if (rdfInputStream == null) {
            // With no rdf body provided, create version from current resource state.
            mementoRdfStream = resource.getTriples(idTranslator, VERSION_TRIPLES);
        } else {
            final Model inputModel = ModelFactory.createDefaultModel();
            inputModel.read(rdfInputStream, mementoUri, rdfFormat.getName());

            if (inputModel.isEmpty()) {
                throw new ConstraintViolationException(
                        "Cannot create historic memento from an empty body");
            }

            // Validate server managed triples are provided
            if (resource instanceof Container) {
                assertRequiredContainerTriples(inputModel);
            } else {
                assertRequiredDescriptionTriples(inputModel);

                // Remove fixity service reference due to disallowed fcr prefix
                inputModel.removeAll(null, HAS_FIXITY_SERVICE, null);
            }

            mementoRdfStream = DefaultRdfStream.fromModel(createURI(mementoUri), inputModel);
        }

        final Session jcrSession = getJcrSession(session);
        final RdfStream mappedStream = remapResourceUris(resourceUri, mementoUri, mementoRdfStream,
                idTranslator, jcrSession);

        new RelaxedRdfAdder(idTranslator, jcrSession, mappedStream, namespaceRegistry.getNamespaces()).consume();

        decorateWithMementoProperties(session, mementoPath, dateTime);

        return mementoResource;
    }

    /*
     * Creates a minimal container node for further population elsewhere
     */
    private Container createContainer(final FedoraSession session, final String path) {
        try {
            final Node node = findOrCreateNode(session, path, NT_FOLDER);

            if (node.canAddMixin(FEDORA_RESOURCE)) {
                node.addMixin(FEDORA_RESOURCE);
                node.addMixin(FEDORA_CONTAINER);
            }

            return new ContainerImpl(node);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Remaps the subjects of triples in rdfStream from the original resource URL to the URL of the new memento, and
     * converts objects which reference resources to an internal identifier to prevent enforcement of referential
     * integrity constraints.
     *
     * @param resourceUri uri of the original resource
     * @param mementoUri uri of the memento resource
     * @param rdfStream rdf stream
     * @param idTranslator translator for producing URI of resources
     * @param jcrSession jcr session
     * @return RdfStream
     */
    private RdfStream remapResourceUris(final String resourceUri,
            final String mementoUri,
            final RdfStream rdfStream,
            final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Session jcrSession) {
        final IdentifierConverter<Resource, FedoraResource> internalIdTranslator = new InternalIdentifierTranslator(
                jcrSession);

        final org.apache.jena.graph.Node mementoNode = createURI(mementoUri);
        final Stream<Triple> mappedStream = rdfStream.map(t -> mapSubject(t, resourceUri, mementoUri))
                .map(t -> convertToInternalReference(t, idTranslator, internalIdTranslator));
        return new DefaultRdfStream(mementoNode, mappedStream);
    }

    /**
     * Convert the referencing resource uri to un-dereferenceable internal identifier.
     *
     * @param t the Triple to convert
     * @param idTranslator the Converter that convert the resource uri to a path
     * @param internalIdTranslator the Converter that convert a path to internal identifier
     * @return Triple a triple with referencing resource uri converted to internal identifier
     */
    private Triple convertToInternalReference(final Triple t,
            final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final IdentifierConverter<Resource, FedoraResource> internalIdTranslator) {
        if (t.getObject().isURI()) {
            final Resource object = createResource(t.getObject().getURI());
            if (idTranslator.inDomain(object)) {
                final String path = idTranslator.convert(object).getPath();
                final Resource obj = createResource(internalIdTranslator.toDomain(path).getURI());
                LOGGER.debug("Converting referencing resource uri {} to internal identifier {}.",
                        t.getObject().getURI(), obj.getURI());

                return new Triple(t.getSubject(), t.getPredicate(), obj.asNode());
            }
        }

        return t;
    }

    @Override
    public FedoraBinary createExternalBinaryVersion(final FedoraSession session,
            final FedoraBinary resource,
            final Instant dateTime,
            final Collection<URI> checksums,
            final String externalHandling,
            final String externalUrl)
            throws InvalidChecksumException {
        final String mementoPath = makeMementoPath(resource, dateTime);
        assertMementoDoesNotExist(session, mementoPath);

        final FedoraBinary memento = binaryService.findOrCreateBinary(session, mementoPath);
        decorateWithMementoProperties(session, mementoPath, dateTime);

        memento.setExternalContent(null, checksums, null, externalHandling, externalUrl);

        return memento;
    }

    @Override
    public FedoraBinary createBinaryVersion(final FedoraSession session,
            final FedoraBinary resource,
            final Instant dateTime,
            final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {
        return createBinaryVersion(session, resource, dateTime, null, null, storagePolicyDecisionPoint);
    }

    @Override
    public FedoraBinary createBinaryVersion(final FedoraSession session,
            final FedoraBinary resource,
            final Instant dateTime,
            final InputStream contentStream,
            final Collection<URI> checksums,
            final StoragePolicyDecisionPoint storagePolicyDecisionPoint) throws InvalidChecksumException {

        final String mementoPath = makeMementoPath(resource, dateTime);
        assertMementoDoesNotExist(session, mementoPath);

        final FedoraBinary memento = binaryService.findOrCreateBinary(session, mementoPath);
        decorateWithMementoProperties(session, mementoPath, dateTime);

        if (contentStream == null) {
            // Creating memento from existing resource
            populateBinaryMementoFromExisting(resource, memento, storagePolicyDecisionPoint);
        } else {
            memento.setContent(contentStream, null, checksums, null, storagePolicyDecisionPoint);
        }

        return memento;
    }

    private void populateBinaryMementoFromExisting(final FedoraBinary resource, final FedoraBinary memento,
            final StoragePolicyDecisionPoint storagePolicyDecisionPoint) throws InvalidChecksumException {

        final Node contentNode = getJcrNode(resource);
        List<URI> checksums = null;
        // Retrieve all existing digests from the original
        try {
            if (contentNode.hasProperty(CONTENT_DIGEST)) {
                final Property digestProperty = contentNode.getProperty(CONTENT_DIGEST);
                checksums = stream(digestProperty.getValues())
                        .map(d -> {
                            try {
                                return URI.create(d.getString());
                            } catch (final RepositoryException e) {
                                throw new RepositoryRuntimeException(e);
                            }
                        }).collect(Collectors.toList());
            }

            // if current binary is external, gather details
            String handling = null;
            String externalUrl = null;
            if (resource.isProxy()) {
                handling = PROXY;
                externalUrl = resource.getProxyURL();
            } else if (resource.isRedirect()) {
                handling = REDIRECT;
                externalUrl = resource.getRedirectURL();
            }

            // Create memento as external or internal based on state of original
            if (handling != null && externalUrl != null) {
                memento.setExternalContent(null, checksums, null, handling, externalUrl);
            } else {
                memento.setContent(resource.getContent(), null, checksums,
                        null, storagePolicyDecisionPoint);
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private String makeMementoPath(final FedoraResource resource, final Instant datetime) {
        return resource.getPath() + "/" + LDPCV_TIME_MAP + "/" + MEMENTO_LABEL_FORMATTER.format(datetime);
    }

    private String getUri(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        if (idTranslator == null) {
            return resource.getPath();
        }
        return idTranslator.reverse().convert(resource).getURI();
    }

    private void decorateWithMementoProperties(final FedoraSession session, final String mementoPath,
                                               final Instant dateTime) {
        try {
            final Node mementoNode = findNode(session, mementoPath);
            if (mementoNode.canAddMixin(MEMENTO)) {
                mementoNode.addMixin(MEMENTO);
            }
            final Calendar mementoDatetime = GregorianCalendar.from(
                    ZonedDateTime.ofInstant(dateTime, ZoneId.of("UTC")));
            mementoNode.setProperty(MEMENTO_DATETIME, mementoDatetime);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void assertMementoDoesNotExist(final FedoraSession session, final String mementoPath) {
        if (exists(session, mementoPath)) {
            throw new RepositoryRuntimeException(new ItemExistsException(
                    "Memento " + mementoPath + " already exists"));
        }
    }
}
