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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;

import org.fcrepo.kernel.api.ExternalContent;
import org.fcrepo.kernel.api.exception.CannotCreateResourceException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.CreateResourceService;
import org.fcrepo.kernel.api.services.functions.UniqueValueSupplier;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import javax.inject.Inject;
import javax.ws.rs.core.Link;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class CreateResourceServiceImpl extends AbstractService implements CreateResourceService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private RdfSourceOperationFactory rdfSourceOperationFactory;

    @Inject
    private UniqueValueSupplier minter;

    @Override
    public void perform(final String txId, final String fedoraId, final String slug, final String contentType,
                        final boolean isRdfContentType, final List<String> linkHeaders, final String digest,
                        final InputStream requestBody, final ExternalContent externalContent) {
        checkAclLinkHeader(linkHeaders);
        final PersistentStorageSession pSession = this.psManager.getSession(txId);
        final ResourceHeaders parent;
        try {
            // Make sure the parent exists.
            parent = pSession.getHeaders(fedoraId, null);
        } catch (PersistentItemNotFoundException exc) {
            throw new ItemNotFoundException(String.format("Item %s was not found", fedoraId), exc);
        }

        final boolean isParentBinary = parent.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase(NON_RDF_SOURCE.toString()));
        if (isParentBinary) {
            // Binary is not a container, can't have children.
            throw new CannotCreateResourceException("NonRdfSource resources cannot contain other resources");
        }
        // TODO: Will this type still be needed?
        final boolean isPairTree = parent.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase(FEDORA_PAIRTREE));
        if (isPairTree) {
            throw new CannotCreateResourceException("Objects cannot be created under pairtree nodes");
        }

        String finalSlug;
        if (slug == null) {
            finalSlug = this.minter.get();
        } else {
            try {
                pSession.getHeaders(fedoraId + "/" + slug, null);
                // Slug exists, so we need to generate a new path.
                finalSlug = this.minter.get();
            } catch (PersistentItemNotFoundException exc) {
                // Doesn't already exist so the slug is fine.
                finalSlug = slug;
            }
        }
        final String fullPath = fedoraId + (fedoraId.endsWith("/") ? "" : "/") + finalSlug;

        hasRestrictedPath(fullPath);

        final String interactionModel = determineInteractionModel(getTypes(linkHeaders), isRdfContentType,
                requestBody != null, externalContent != null);

        final ResourceOperation createOp;

        if (interactionModel.equals(NON_RDF_SOURCE.toString())) {
            // TODO: Implement the NonRdfSourceOperationFactory
            createOp = null;
        } else {
            createOp = rdfSourceOperationFactory.createBuilder(fullPath, interactionModel)
                    .triples(requestBody, contentType).build();
        }
        try {
            pSession.persist(createOp);
        } catch (PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create resource %s", fedoraId), exc);
        }
    }

    /**
     * Get the rel="type" link headers from a list of them.
     * @param headers a list of string LINK headers.
     * @return a list of LINK headers with rel="type"
     */
    private List<String> getTypes(final List<String> headers) {
        final List<String> types = getLinkHeaders(headers) == null ? null : getLinkHeaders(headers).stream()
                .filter(p -> p.getRel().equalsIgnoreCase("type")).map(Link::getUri)
                .map(URI::toString).collect(Collectors.toList());
        return types;
    }

    /**
     * Converts a list of string LINK headers to actual LINK objects.
     * @param headers the list of string link headers.
     * @return the list of LINK headers.
     */
    private List<Link> getLinkHeaders(final List<String> headers) {
        return headers == null ? null : headers.stream().map(p -> Link.fromUri(p).build()).collect(Collectors.toList());
    }
}
