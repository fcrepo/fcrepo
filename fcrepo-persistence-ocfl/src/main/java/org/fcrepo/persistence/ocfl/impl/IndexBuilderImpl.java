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
package org.fcrepo.persistence.ocfl.impl;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.getRdfFormat;

import edu.wisc.library.ocfl.api.OcflRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.Condition.Field;
import org.fcrepo.search.api.Condition.Operator;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link IndexBuilder}.  This implementation rebuilds the following indexable state derived
 * from the underlying OCFL directory:
 * 1) the link between a {@link org.fcrepo.kernel.api.identifiers.FedoraId} and an OCFL object identifier
 * 2) the containment relationships bewteen {@link org.fcrepo.kernel.api.identifiers.FedoraId}s
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Component
public class IndexBuilderImpl implements IndexBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexBuilderImpl.class);

    private int membershipPageSize = 500;

    @Inject
    private OcflObjectSessionFactory objectSessionFactory;

    @Inject
    private FedoraToOcflObjectIndex fedoraToOcflObjectIndex;

    @Inject
    private ContainmentIndex containmentIndex;

    @Inject
    private SearchIndex searchIndex;

    @Inject
    private OcflRepository ocflRepository;

    @Inject
    private ReferenceService referenceService;

    @Inject
    private MembershipService membershipService;

    @Override
    public void rebuildIfNecessary() {
        if (shouldRebuild()) {
            rebuild();
        } else {
            LOGGER.debug("No index rebuild necessary");
        }
    }

    private void rebuild() {
        LOGGER.info("Initiating index rebuild.");

        fedoraToOcflObjectIndex.reset();
        containmentIndex.reset();
        searchIndex.reset();
        referenceService.reset();
        membershipService.reset();

        final var txId = UUID.randomUUID().toString();

        try {
            LOGGER.debug("Reading object ids...");

            try (final var ocflIds = ocflRepository.listObjectIds()) {
                ocflIds.forEach(ocflId -> {
                    LOGGER.debug("Reading {}", ocflId);
                    try (final var session = objectSessionFactory.newSession(ocflId)) {
                        indexOcflObject(ocflId, txId, session);
                    } catch (final Exception e) {
                        // The session's close method signature throws Exception
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        }
                        throw new RuntimeException(e);
                    }
                });
            }

            containmentIndex.commitTransaction(txId);
            fedoraToOcflObjectIndex.commit(txId);
            referenceService.commitTransaction(txId);
            indexMembership(txId);
            LOGGER.info("Index rebuild complete");
        } catch (final RuntimeException e) {
            execQuietly("Failed to reset searchIndex", () -> {
                searchIndex.reset();
                return null;
            });

            execQuietly("Failed to rollback containment index transaction " + txId, () -> {
                containmentIndex.rollbackTransaction(txId);
                return null;
            });
            execQuietly("Failed to rollback OCFL index transaction " + txId, () -> {
                fedoraToOcflObjectIndex.rollback(txId);
                return null;
            });

            execQuietly("Failed to rollback membership index transaction " + txId, () -> {
                membershipService.rollbackTransaction(txId);
                return null;
            });
            throw e;
        }
    }

    private void indexOcflObject(final String ocflId, final String txId, final OcflObjectSession session) {
        final var rootId = new AtomicReference<FedoraId>();
        final var fedoraIds = new ArrayList<FedoraId>();
        final var headersList = new ArrayList<ResourceHeaders>();

        session.streamResourceHeaders().forEach(storageHeaders -> {
            final var headers = new ResourceHeadersAdapter(storageHeaders);

            final var fedoraId = headers.getId();
            fedoraIds.add(fedoraId);
            if (headers.isArchivalGroup() || headers.isObjectRoot()) {
                rootId.set(fedoraId);
            }

            if (!headers.isDeleted() && !fedoraId.isRepositoryRoot()) {
                var parentId = headers.getParent();

                if (headers.getParent() == null) {
                    if (headers.isObjectRoot()) {
                        parentId = FedoraId.getRepositoryRootId();
                    } else {
                        throw new IllegalStateException(String.format("Resource %s must have a parent defined",
                                fedoraId.getFullId()));
                    }
                }
                if (!headers.getInteractionModel().equals(NON_RDF_SOURCE.toString())) {
                    final Optional<InputStream> content = session.readContent(fedoraId.getFullId()).getContentStream();
                    if (content.isPresent()) {
                        final RdfStream rdf = parseRdf(fedoraId, content.get());
                        this.referenceService.updateReferences(txId, fedoraId, null, rdf);
                    }
                }

                this.containmentIndex.addContainedBy(txId, parentId, fedoraId);
                headersList.add(headers.asKernelHeaders());
            }
        });

        if (rootId.get() == null) {
            throw new IllegalStateException(String.format("Failed to find root resource in object %s", ocflId));
        }

        fedoraIds.forEach(fedoraIdentifier -> {
            final var rootFedoraIdentifier = rootId.get();
            fedoraToOcflObjectIndex.addMapping(txId, fedoraIdentifier, rootFedoraIdentifier, ocflId);
            LOGGER.debug("Rebuilt fedora-to-ocfl object index entry for {}", fedoraIdentifier);
        });

        headersList.forEach(headers -> {
            searchIndex.addUpdateIndex(txId, headers);
            LOGGER.debug("Rebuilt searchIndex for {}", headers.getId());
        });
    }

    /**
     * Index all membership properties by querying for Direct containers, and then
     * trying population of the membership index for each one
     * @param txId
     */
    private void indexMembership(final String txId) {
        final var fields = List.of(Condition.Field.FEDORA_ID);
        final var conditions = List.of(Condition.fromEnums(Field.RDF_TYPE, Operator.EQ,
                RdfLexicon.DIRECT_CONTAINER.getURI()));
        int offset = 0;

        try {
            int numResults = membershipPageSize;
            do {
                final var params = new SearchParameters(fields, conditions, membershipPageSize,
                        offset, Field.FEDORA_ID, "asc");

                final var searchResult = searchIndex.doSearch(params);
                final var resultList = searchResult.getItems();
                numResults = resultList.size();

                resultList.stream()
                    .map(entry -> FedoraId.create((String) entry.get(Condition.Field.FEDORA_ID.toString())))
                    .forEach(containerId -> membershipService.populateMembershipHistory(txId, containerId));

                // Results are paged, so step through pages until we reach the last one
                offset += membershipPageSize;
            } while (numResults == membershipPageSize);

        } catch (final InvalidQueryException e) {
            throw new RepositoryRuntimeException("Failed to repopulate membership history", e);
        }
        membershipService.commitTransaction(txId);
    }

    /**
     * @param pageSize number of results to use when querying for membership producing resources
     */
    public void setMembershipQueryPageSize(final int pageSize) {
        this.membershipPageSize = pageSize;
    }

    private boolean shouldRebuild() {
        final var repoContainsObjects = repoContainsObjects();
        final var repoRootMappingExists = repoRootMappingExists();
        final var repoRootContainmentExists = repoRootContainmentExists();

        return (repoContainsObjects && (!repoRootMappingExists || !repoRootContainmentExists)
                || (!repoContainsObjects && (repoRootMappingExists || repoRootContainmentExists)));
    }

    private boolean repoRootMappingExists() {
        try {
            return fedoraToOcflObjectIndex.getMapping(null, FedoraId.getRepositoryRootId()) != null;
        } catch (final FedoraOcflMappingNotFoundException e) {
            return false;
        }
    }

    private boolean repoRootContainmentExists() {
        return containmentIndex.resourceExists(null, FedoraId.getRepositoryRootId());
    }

    private boolean repoContainsObjects() {
        return ocflRepository.listObjectIds().findFirst().isPresent();
    }

    private static RdfStream parseRdf(final FedoraId fedoraIdentifier, final InputStream inputStream) {
        final Model model = createDefaultModel();
        RDFDataMgr.read(model, inputStream, getRdfFormat().getLang());
        final FedoraId topic = (fedoraIdentifier.isDescription() ? fedoraIdentifier.asBaseId() : fedoraIdentifier);
        return DefaultRdfStream.fromModel(createURI(topic.getFullId()), model);
    }

    /**
     * Executes the closure, capturing all exceptions, and logging them as errors.
     *
     * @param failureMessage what to print if the closure fails
     * @param callable closure to execute
     */
    private void execQuietly(final String failureMessage, final Callable<Void> callable) {
        try {
            callable.call();
        } catch (final Exception e) {
            LOGGER.error(failureMessage, e);
        }
    }

}
