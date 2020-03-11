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

import static java.util.Collections.emptyList;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_PAIR_TREE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.fcrepo.kernel.impl.services.functions.FedoraIdUtils.addToIdentifier;
import static org.fcrepo.kernel.impl.services.functions.FedoraIdUtils.ensurePrefix;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Link;

import org.apache.jena.rdf.model.Model;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateNonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.CreateResourceService;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

/**
 * Create a RdfSource resource.
 * @author whikloj
 * TODO: bbpennel has thoughts about moving this to HTTP layer.
 */
@Component
public class CreateResourceServiceImpl extends AbstractService implements CreateResourceService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private RdfSourceOperationFactory rdfSourceOperationFactory;

    @Inject
    private NonRdfSourceOperationFactory nonRdfSourceOperationFactory;

    @Inject
    private UniqueValueSupplier minter;

    @Override
    public String perform(final String txId, final String userPrincipal, final String fedoraId, final String slug,
                        final boolean isContained, final String contentType, final String filename,
                        final Long contentSize, final List<String> linkHeaders, final Collection<URI> digest,
                        final InputStream requestBody, final ExternalContent externalContent) {
        final String[] parts = splitSlug(txId, ensurePrefix(fedoraId), slug);
        // If we found an existing resource in the Slug, then that is our parent.
        final String normalizedFedoraId = (parts[0] != null) ? addToIdentifier(ensurePrefix(fedoraId), parts[0]) :
                    ensurePrefix(fedoraId);
        final String realSlug = parts[1];
        final PersistentStorageSession pSession = this.psManager.getSession(txId);
        checkAclLinkHeader(linkHeaders);
        // If we are PUTting then fedoraId is the path, we need to locate a containment parent if exists.
        // Otherwise we use fedoraId and create a resource contained in it.
        final String parentId = isContained ? normalizedFedoraId : findExistingAncestor(txId, normalizedFedoraId);
        checkParent(txId, pSession, parentId);

        final String fullPath = isContained ? getResourcePath(txId, pSession, normalizedFedoraId, realSlug) :
                normalizedFedoraId;

        // Populate the description for the new binary
        createDescription(pSession, userPrincipal, fullPath);

        final CreateNonRdfSourceOperationBuilder builder;
        String mimeType = contentType;
        if (externalContent == null) {
            builder = nonRdfSourceOperationFactory.createInternalBinaryBuilder(fullPath, requestBody);
        } else {
            builder = nonRdfSourceOperationFactory.createExternalBinaryBuilder(fullPath, externalContent.getHandling(),
                    URI.create(externalContent.getURL()));
            if (externalContent.getContentType() != null) {
                mimeType = externalContent.getContentType();
            }
        }
        final ResourceOperation createOp = builder
                .parentId(parentId)
                .userPrincipal(userPrincipal)
                .contentDigests(digest)
                .mimeType(mimeType)
                .contentSize(contentSize)
                .filename(filename)
                .build();

        try {
            pSession.persist(createOp);
            addToContainmentIndex(txId, parentId, fullPath);
            return fullPath;
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create resource %s", fedoraId), exc);
        }
    }

    private String createDescription(final PersistentStorageSession pSession, final String userPrincipal,
            final String binaryId) {
        final var descId = binaryId + "/" + FCR_METADATA;
        final var createOp = rdfSourceOperationFactory.createBuilder(descId, FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI)
                .userPrincipal(userPrincipal)
                .build();
        try {
            pSession.persist(createOp);
            return descId;
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create description %s", descId), exc);
        }
    }

    @Override
    public String perform(final String txId, final String userPrincipal, final String fedoraId, final String slug,
            final boolean isContained, final List<String> linkHeaders, final Model model) {
        final String[] parts = splitSlug(txId, ensurePrefix(fedoraId), slug);
        // If we found an existing resource in the Slug, then that is our parent.
        final String normalizedFedoraId = (parts[0] != null) ? addToIdentifier(ensurePrefix(fedoraId), parts[0]) :
                ensurePrefix(fedoraId);
        final String realSlug = parts[1];
        final PersistentStorageSession pSession = this.psManager.getSession(txId);
        checkAclLinkHeader(linkHeaders);
        // If we are PUTting then fedoraId is the path, we need to locate a containment parent if exists.
        // Otherwise we use fedoraId and create a resource contained in it.
        final String parentId = isContained ? normalizedFedoraId : findExistingAncestor(txId, normalizedFedoraId);
        checkParent(txId, pSession, parentId);
        final String fullPath = isContained ? getResourcePath(txId, pSession, normalizedFedoraId, realSlug) :
                normalizedFedoraId;

        final List<String> rdfTypes = isEmpty(linkHeaders) ? emptyList() : getTypes(linkHeaders);
        final String interactionModel = determineInteractionModel(rdfTypes, true,
                model != null, false);

        final RdfStream stream = fromModel(model.getResource(fedoraId).asNode(), model);

        final RdfSourceOperation createOp = rdfSourceOperationFactory.createBuilder(fullPath, interactionModel)
                .parentId(parentId)
                .triples(stream)
                .relaxedProperties(model)
                .archivalGroup(rdfTypes.contains(ARCHIVAL_GROUP.getURI()))
                .build();

        try {
            pSession.persist(createOp);
            addToContainmentIndex(txId, parentId, fullPath);
            return fullPath;
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create resource %s", fedoraId), exc);
        }

    }

    /**
     * Check the parent to contain the new resource exists and can have a child.
     *
     * @param txId the transaction ID or null if no transaction.
     * @param pSession a persistence session.
     * @param fedoraId Id of parent or null if root.
     */
    private void checkParent(final String txId, final PersistentStorageSession pSession, final String fedoraId)
        throws RepositoryRuntimeException {

        if (fedoraId != null) {
            final ResourceHeaders parent;
            try {
                // Make sure the parent exists.
                final boolean parentExists = containmentIndex.resourceExists(txId, fedoraId);
                // TODO: object existence check could be from an index. Review later.
                parent = pSession.getHeaders(fedoraId, null);
            } catch (final PersistentItemNotFoundException exc) {
                throw new ItemNotFoundException(String.format("Item %s was not found", fedoraId), exc);
            } catch (final PersistentStorageException exc) {
                throw new RepositoryRuntimeException(String.format("Failed to find storage headers for %s", fedoraId),
                    exc);
            }

            final boolean isParentBinary = NON_RDF_SOURCE.toString().equals(parent.getInteractionModel());
            if (isParentBinary) {
                // Binary is not a container, can't have children.
                throw new CannotCreateResourceException("NonRdfSource resources cannot contain other resources");
            }
            // TODO: Will this type still be needed?
            final boolean isPairTree = FEDORA_PAIR_TREE.toString().equals(parent.getInteractionModel());
            if (isPairTree) {
                throw new CannotCreateResourceException("Objects cannot be created under pairtree nodes");
            }
        }
    }

    /**
     * Get the path of a new child with or without a slug.
     * @param txId a transaction id or null.
     * @param pSession a persistent storage session.
     * @param fedoraId the current parent's identifier.
     * @param slug a provided slug or null
     * @return the new identifier.
     */
    private String getResourcePath(final String txId, final PersistentStorageSession pSession, final String fedoraId,
                                   final String slug)
        throws RepositoryRuntimeException {

        final String fullTestPath = addToIdentifier(fedoraId, slug);
        if (slug == null || containmentIndex.resourceExists(txId, fullTestPath)) {
            // Resource with slug name exists already, so we need to generate a new path.
            return addToIdentifier(fedoraId, this.minter.get());
        } else {
            return addToIdentifier(fedoraId, slug);
        }
    }

    /**
     * Get the rel="type" link headers from a list of them.
     * @param headers a list of string LINK headers.
     * @return a list of LINK headers with rel="type"
     */
    private List<String> getTypes(final List<String> headers) {
        return getLinkHeaders(headers) == null ? emptyList() : getLinkHeaders(headers).stream()
                .filter(p -> p.getRel().equalsIgnoreCase("type")).map(Link::getUri)
                .map(URI::toString).collect(Collectors.toList());
    }

    /**
     * Converts a list of string LINK headers to actual LINK objects.
     * @param headers the list of string link headers.
     * @return the list of LINK headers.
     */
    private List<Link> getLinkHeaders(final List<String> headers) {
        return headers == null ? null : headers.stream().map(p -> Link.valueOf(p)).collect(Collectors.toList());
    }

    /**
     * Add this pairing to the containment index.
     * @param txId The transaction ID or null if no transaction.
     * @param parentId The parent ID or null if repository root.
     * @param id The child ID.
     */
    private void addToContainmentIndex(final String txId, final String parentId, final String id) {
        containmentIndex.addContainedBy(txId, ensurePrefix(parentId), ensurePrefix(id));
    }

    /**
     * If the Slug has a forward slash then check that part of it is not an existing resource.
     * @param txID The current transaction id.
     * @param parent The fedora ID passed in with the slug.
     * @param slug The slug.
     * @return Array with the parent and slug.
     */
    private String[] splitSlug(final String txID, final String parent, final String slug) {
        if (slug != null && slug.contains("/")) {
            String slugIterator = slug;
            while (slugIterator.contains("/")) {
                final String testParent = slug.substring(0, slugIterator.lastIndexOf("/"));
                slugIterator = slug.substring(0, slugIterator.lastIndexOf("/"));
                if (containmentIndex.resourceExists(txID, addToIdentifier(parent, testParent))) {
                    final String[] response = { testParent, slug.substring(testParent.length() + 1) };
                    return response;
                }
            }
        }
        final String[] response = { null, slug };
        return response;
    }
}
