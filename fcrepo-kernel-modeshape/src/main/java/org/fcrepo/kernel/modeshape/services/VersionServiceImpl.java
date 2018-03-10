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

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.modeshape.utils.iterators.RelaxedRdfAdder;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_MEMENTO;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_MEMENTO_DATETIME;
import static org.fcrepo.kernel.modeshape.FedoraResourceImpl.LDPCV_TIME_MAP;
import static org.fcrepo.kernel.api.RequiredRdfContext.EMBED_RESOURCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_CONTAINMENT;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_MEMBERSHIP;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This service exposes management of node versioning for resources and binaries.
 *
 * @author Mike Durbin
 * @author bbpennel
 */

@Component
public class VersionServiceImpl extends AbstractService implements VersionService {

    private static final Logger LOGGER = getLogger(VersionService.class);

    private static final DateTimeFormatter MEMENTO_DATETIME_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("GMT"));

    private static final Set<TripleCategory> VERSION_TRIPLES = new HashSet<>(asList(
            PROPERTIES, EMBED_RESOURCES, SERVER_MANAGED, LDP_MEMBERSHIP, LDP_CONTAINMENT));

    /**
     * The repository object service
     */
    @Inject
    protected ContainerService containerService;

    /**
     * The bitstream service
     */
    @Inject
    protected BinaryService binaryService;

    @Inject
    protected NodeService nodeService;

    @Override
    public FedoraResource createVersion(final FedoraSession session,
            final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator,
            final Instant dateTime,
            final RdfStream rdfStream) {

        final String mementoPath = makeMementoPath(resource, dateTime);

        assertMementoDoesNotExist(session, mementoPath);

        final FedoraResource mementoResource = containerService.findOrCreate(session, mementoPath);
        final String mementoUri = getUri(mementoResource, idTranslator);

        decorateWithMementoProperties(session, mementoPath, dateTime);

        final RdfStream mementoRdfStream;
        if (rdfStream == null) {
            // With no rdf body provided, create version from current resource state.
            mementoRdfStream = resource.getTriples(idTranslator, VERSION_TRIPLES);
        } else {
            // Replace original subject in incoming RDF with memento subject
            final String resourceUri = getUri(resource, idTranslator);
            mementoRdfStream = remapRdfSubjects(mementoUri, resourceUri, rdfStream);
        }

        final Session jcrSession = getJcrSession(session);
        new RelaxedRdfAdder(idTranslator, jcrSession, mementoRdfStream, session.getNamespaces()).consume();

        return mementoResource;
    }

    private RdfStream remapRdfSubjects(final String mementoUri, final String resourceUri, final RdfStream rdfStream) {
        final org.apache.jena.graph.Node mementoNode = createURI(mementoUri);
        final Stream<Triple> updatedSubjectStream = rdfStream.map(t -> {
            final org.apache.jena.graph.Node subject;
            if (t.getSubject().getURI().equals(resourceUri)) {
                subject = mementoNode;
            } else {
                subject = t.getSubject();
            }
            return new Triple(subject, t.getPredicate(), t.getObject());
        });
        return new DefaultRdfStream(mementoNode, updatedSubjectStream);
    }

    @Override
    public FedoraResource createBinaryVersion(final FedoraSession session,
            final FedoraResource resource,
            final Instant dateTime,
            final InputStream contentStream,
            final String filename,
            final String mimetype,
            final Collection<URI> checksums) {

        final String mementoPath = makeMementoPath(resource, dateTime);

        assertMementoDoesNotExist(session, mementoPath);

        LOGGER.debug("Creating memento {} for resource {} using existing state", mementoPath, resource.getPath());
        nodeService.copyObject(session, resource.getPath(), mementoPath);

        final FedoraBinary memento = binaryService.findOrCreate(session, mementoPath);

        decorateWithMementoProperties(session, mementoPath, dateTime);

        return memento;
    }

    private String makeMementoPath(final FedoraResource resource, final Instant datetime) {
        return resource.getPath() + "/" + LDPCV_TIME_MAP + "/" + MEMENTO_DATETIME_ID_FORMATTER.format(datetime);
    }

    protected String getUri(final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator) {
        if (idTranslator == null) {
            return resource.getPath();
        }
        return idTranslator.reverse().convert(resource).getURI();
    }

    protected void decorateWithMementoProperties(final FedoraSession session, final String mementoPath,
            final Instant dateTime) {
        try {
            final Node mementoNode = findNode(session, mementoPath);
            if (mementoNode.canAddMixin(FEDORA_MEMENTO)) {
                mementoNode.addMixin(FEDORA_MEMENTO);
            }
            final Calendar mementoDatetime = GregorianCalendar.from(
                    ZonedDateTime.ofInstant(dateTime, ZoneId.of("UTC")));
            mementoNode.setProperty(FEDORA_MEMENTO_DATETIME, mementoDatetime);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected void assertMementoDoesNotExist(final FedoraSession session, final String mementoPath) {
        if (exists(session, mementoPath)) {
            throw new RepositoryRuntimeException(new ItemExistsException(
                    "Memento " + mementoPath + " already exists"));
        }
    }
}
