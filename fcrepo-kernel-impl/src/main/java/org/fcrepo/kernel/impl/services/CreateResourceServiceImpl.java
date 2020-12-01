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
package org.fcrepo.kernel.impl.services;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.InteractionModelViolationException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateNonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.CreateResourceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.MultiDigestInputStreamWrapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Link;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_PAIR_TREE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Create a RdfSource resource.
 * @author whikloj
 * TODO: bbpennel has thoughts about moving this to HTTP layer.
 */
@Component
public class CreateResourceServiceImpl extends AbstractService implements CreateResourceService {

    private static final Logger LOGGER = getLogger(CreateResourceServiceImpl.class);

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private RdfSourceOperationFactory rdfSourceOperationFactory;

    @Inject
    private NonRdfSourceOperationFactory nonRdfSourceOperationFactory;

    @Override
    public void perform(final Transaction tx, final String userPrincipal, final FedoraId fedoraId,
                        final String contentType, final String filename,
                        final long contentSize, final List<String> linkHeaders, final Collection<URI> digest,
                        final InputStream requestBody, final ExternalContent externalContent) {
        final var txId = tx.getId();
        final PersistentStorageSession pSession = this.psManager.getSession(txId);
        checkAclLinkHeader(linkHeaders);
        // Locate a containment parent of fedoraId, if exists.
        final FedoraId parentId = containmentIndex.getContainerIdByPath(txId, fedoraId, true);
        checkParent(pSession, parentId);

        final CreateNonRdfSourceOperationBuilder builder;
        String mimeType = contentType;
        long size = contentSize;
        if (externalContent == null || externalContent.isCopy()) {
            var contentInputStream = requestBody;
            if (externalContent != null) {
                LOGGER.debug("External content COPY '{}', '{}'", fedoraId, externalContent.getURL());
                contentInputStream = externalContent.fetchExternalContent();
            }

            builder = nonRdfSourceOperationFactory.createInternalBinaryBuilder(fedoraId, contentInputStream);
        } else {
            builder = nonRdfSourceOperationFactory.createExternalBinaryBuilder(fedoraId,
                    externalContent.getHandling(), externalContent.getURI());
            if (contentSize == -1L) {
                size = externalContent.getContentSize();
            }
            if (!digest.isEmpty()) {
                final var multiDigestWrapper = new MultiDigestInputStreamWrapper(
                        externalContent.fetchExternalContent(),
                        digest,
                        Collections.emptyList());
                multiDigestWrapper.checkFixity();
            }
        }

        if (externalContent != null && externalContent.getContentType() != null) {
            mimeType = externalContent.getContentType();
        }

        final ResourceOperation createOp = builder
                .parentId(parentId)
                .userPrincipal(userPrincipal)
                .contentDigests(digest)
                .mimeType(mimeType)
                .contentSize(size)
                .filename(filename)
                .build();

        lockArchivalGroupResourceFromParent(tx, pSession, parentId);
        tx.lockResource(fedoraId);

        try {
            pSession.persist(createOp);
            // Populate the description for the new binary
            createDescription(tx, pSession, userPrincipal, fedoraId);
            addToContainmentIndex(txId, parentId, fedoraId);
            membershipService.resourceCreated(txId, fedoraId);
            recordEvent(txId, fedoraId, createOp);
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create resource %s", fedoraId), exc);
        }
    }

    private void createDescription(final Transaction tx,
                                   final PersistentStorageSession pSession,
                                   final String userPrincipal,
                                   final FedoraId binaryId) {
        final var descId = binaryId.asDescription();
        final var createOp = rdfSourceOperationFactory.createBuilder(
                    descId,
                    FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI
                ).userPrincipal(userPrincipal)
                .parentId(binaryId)
                .build();

        tx.lockResource(descId);

        try {
            pSession.persist(createOp);
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create description %s", descId), exc);
        }
    }

    @Override
    public void perform(final Transaction tx, final String userPrincipal, final FedoraId fedoraId,
            final List<String> linkHeaders, final Model model) {
        final var txId = tx.getId();
        final PersistentStorageSession pSession = this.psManager.getSession(txId);
        checkAclLinkHeader(linkHeaders);
        // Locate a containment parent of fedoraId, if exists.
        final FedoraId parentId = containmentIndex.getContainerIdByPath(txId, fedoraId, true);
        checkParent(pSession, parentId);

        final List<String> rdfTypes = isEmpty(linkHeaders) ? emptyList() : getTypes(linkHeaders);
        final String interactionModel = determineInteractionModel(rdfTypes, true,
                model != null, false);

        final RdfStream stream = fromModel(model.getResource(fedoraId.getFullId()).asNode(), model);

        ensureValidDirectContainer(fedoraId, interactionModel, model);

        final RdfSourceOperation createOp = rdfSourceOperationFactory
                .createBuilder(fedoraId, interactionModel)
                .parentId(parentId)
                .triples(stream)
                .relaxedProperties(model)
                .archivalGroup(rdfTypes.contains(ARCHIVAL_GROUP.getURI()))
                .userPrincipal(userPrincipal)
                .build();

        lockArchivalGroupResourceFromParent(tx, pSession, parentId);
        tx.lockResource(fedoraId);

        try {
            pSession.persist(createOp);
            updateReferences(txId, fedoraId, userPrincipal, model);
            addToContainmentIndex(txId, parentId, fedoraId);
            membershipService.resourceCreated(txId, fedoraId);
            recordEvent(txId, fedoraId, createOp);
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create resource %s", fedoraId), exc);
        }

    }

    /**
     * Check the parent to contain the new resource exists and can have a child.
     *
     * @param pSession a persistence session.
     * @param fedoraId Id of parent.
     */
    private void checkParent(final PersistentStorageSession pSession, final FedoraId fedoraId)
        throws RepositoryRuntimeException {

        if (fedoraId != null && !fedoraId.isRepositoryRoot()) {
            final ResourceHeaders parent;
            try {
                // Make sure the parent exists.
                // TODO: object existence can be from the index, but we don't have interaction model. Should we add it?
                parent = pSession.getHeaders(fedoraId.asResourceId(), null);
            } catch (final PersistentItemNotFoundException exc) {
                throw new ItemNotFoundException(String.format("Item %s was not found", fedoraId), exc);
            } catch (final PersistentStorageException exc) {
                throw new RepositoryRuntimeException(String.format("Failed to find storage headers for %s", fedoraId),
                    exc);
            }
            if (parent.isDeleted()) {
                throw new CannotCreateResourceException(
                        String.format("Cannot create resource as child of a tombstone. Tombstone found at %s",
                                fedoraId.getFullIdPath()));
            }
            final boolean isParentBinary = NON_RDF_SOURCE.toString().equals(parent.getInteractionModel());
            if (isParentBinary) {
                // Binary is not a container, can't have children.
                throw new InteractionModelViolationException("NonRdfSource resources cannot contain other resources");
            }
            // TODO: Will this type still be needed?
            final boolean isPairTree = FEDORA_PAIR_TREE.toString().equals(parent.getInteractionModel());
            if (isPairTree) {
                throw new CannotCreateResourceException("Objects cannot be created under pairtree nodes");
            }
        }
    }

    /**
     * Get the rel="type" link headers from a list of them.
     * @param headers a list of string LINK headers.
     * @return a list of LINK headers with rel="type"
     */
    private List<String> getTypes(final List<String> headers) {
        final List<Link> hdrobjs = getLinkHeaders(headers);
        try {
            return hdrobjs == null ? emptyList() : hdrobjs.stream()
                    .filter(p -> p.getRel().equalsIgnoreCase("type")).map(Link::getUri)
                    .map(URI::toString).collect(Collectors.toList());
        } catch (final Exception e ) {
            throw new BadRequestException("Invalid Link header type found",e);
        }
    }

    /**
     * Converts a list of string LINK headers to actual LINK objects.
     * @param headers the list of string link headers.
     * @return the list of LINK headers.
     */
    private List<Link> getLinkHeaders(final List<String> headers) {
        return headers == null ? null : headers.stream().map(Link::valueOf).collect(Collectors.toList());
    }

    /**
     * Add this pairing to the containment index.
     * @param txId The transaction ID or null if no transaction.
     * @param parentId The parent ID.
     * @param id The child ID.
     */
    private void addToContainmentIndex(final String txId, final FedoraId parentId, final FedoraId id) {
        containmentIndex.addContainedBy(txId, parentId, id);
    }
}
