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
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.getRdfFormat;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service that does the reindexing for one OCFL object.
 * @author whikloj
 */
@Component
public class ReindexService {

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Inject
    private OcflObjectSessionFactory ocflObjectSessionFactory;

    @Autowired
    @Qualifier("ocflIndex")
    private FedoraToOcflObjectIndex ocflIndex;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Autowired
    @Qualifier("searchIndex")
    private SearchIndex searchIndex;

    @Autowired
    @Qualifier("referenceService")
    private ReferenceService referenceService;

    @Inject
    private MembershipService membershipService;

    private static final Logger LOGGER = getLogger(ReindexService.class);

    private int membershipPageSize = 500;

    public void indexOcflObject(final String txId, final String ocflId) {
        LOGGER.debug("Indexing ocflId {} in transaction {}", ocflId, txId);
        try (final var session = ocflObjectSessionFactory.newSession(ocflId)) {
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

                if (!fedoraId.isRepositoryRoot()) {
                    var parentId = headers.getParent();

                    if (headers.getParent() == null) {
                        if (headers.isObjectRoot()) {
                            parentId = FedoraId.getRepositoryRootId();
                        } else {
                            throw new IllegalStateException(
                                    String.format("Resource %s must have a parent defined", fedoraId.getFullId()));
                        }
                    }
                    final var created = headers.getCreatedDate();
                    if (!headers.isDeleted()) {
                        if (!headers.getInteractionModel().equals(NON_RDF_SOURCE.toString())) {
                            final Optional<InputStream> content = session.readContent(fedoraId.getFullId())
                                    .getContentStream();
                            if (content.isPresent()) {
                                try (final var stream = content.get()) {
                                    final RdfStream rdf = parseRdf(fedoraId, stream);
                                    this.referenceService.updateReferences(txId, fedoraId, null, rdf);
                                } catch (final IOException e) {
                                    LOGGER.warn("Content stream for {} closed prematurely, inbound references skipped.",
                                            fedoraId.getFullId());
                                    throw new RepositoryRuntimeException(e.getMessage(), e);
                                }
                            }
                        }

                        this.containmentIndex.addContainedBy(txId, parentId, fedoraId, created, null);
                        headersList.add(headers.asKernelHeaders());
                    } else {
                        final var deleted = headers.getLastModifiedDate();
                        this.containmentIndex.addContainedBy(txId, parentId, fedoraId, created, deleted);
                    }
                }
            });

            if (rootId.get() == null) {
                throw new IllegalStateException(String.format("Failed to find the root resource in object " +
                        "identified by %s. Please ensure that the object ID you are attempting to index " +
                        "refers to a corresponding valid Fedora-flavored object in the OCFL repository. Additionally " +
                        "be sure that the object ID corresponds with the object root resource (as opposed to child " +
                        "resources within the object).", ocflId));
            }

            fedoraIds.forEach(fedoraIdentifier -> {
                final var rootFedoraIdentifier = rootId.get();
                ocflIndex.addMapping(txId, fedoraIdentifier, rootFedoraIdentifier, ocflId);
                LOGGER.debug("Rebuilt fedora-to-ocfl object index entry for {}", fedoraIdentifier);
            });

            headersList.forEach(headers -> {
                searchIndex.addUpdateIndex(txId, headers);
                LOGGER.debug("Rebuilt searchIndex for {}", headers.getId());
            });
        }
    }

    /**
     * Remove persistent sessions for a transaction to avoid memory leaks.
     * @param transactionId the transaction id.
     */
    public void cleanupSession(final String transactionId) {
        persistentStorageSessionManager.removeSession(transactionId);
    }

    /**
     * Set the membership page size.
     * @param pageSize the new page size.
     */
    public void setMembershipPageSize(final int pageSize) {
        membershipPageSize = pageSize;
    }

    /**
     * Reset all the indexes.
     */
    public void reset() {
        ocflIndex.reset();
        containmentIndex.reset();
        searchIndex.reset();
        referenceService.reset();
        membershipService.reset();
    }

    /**
     * Commit the records added from transaction.
     * @param transactionId the id of the transaction.
     */
    public void commit(final String transactionId) {
        try {
            LOGGER.debug("Performing commit of transaction {}", transactionId);
            containmentIndex.commitTransaction(transactionId);
            ocflIndex.commit(transactionId);
            referenceService.commitTransaction(transactionId);
            LOGGER.debug("Finished commit of transaction {}", transactionId);
        } catch (final RuntimeException e) {
            rollback(transactionId);
            throw e;
        }
    }

    /**
     * Quietly rollback all changes to the various indexes.
     * @param transactionId the transaction to rollback.
     */
    public void rollback(final String transactionId) {
        execQuietly("Failed to reset searchIndex", () -> {
            searchIndex.reset();
            return null;
        });

        execQuietly("Failed to rollback containment index transaction " + transactionId, () -> {
            containmentIndex.rollbackTransaction(transactionId);
            return null;
        });
        execQuietly("Failed to rollback OCFL index transaction " + transactionId, () -> {
            ocflIndex.rollback(transactionId);
            return null;
        });

        execQuietly("Failed to rollback the reference index transaction " + transactionId, () -> {
            referenceService.rollbackTransaction(transactionId);
            return null;
        });

        execQuietly("Failed to rollback membership index transaction " + transactionId, () -> {
            membershipService.rollbackTransaction(transactionId);
            return null;
        });
    }

    /**
     * Index all membership properties by querying for Direct containers, and then
     * trying population of the membership index for each one
     * @param txId the transaction id.
     */
    public void indexMembership(final String txId) {
        LOGGER.debug("Starting indexMembership for transaction {}", txId);
        final var fields = List.of(Condition.Field.FEDORA_ID);
        final var conditions = List.of(Condition.fromEnums(Condition.Field.RDF_TYPE, Condition.Operator.EQ,
                RdfLexicon.DIRECT_CONTAINER.getURI()));
        int offset = 0;

        try {
            int numResults;
            do {
                final var params = new SearchParameters(fields, conditions, membershipPageSize,
                        offset, Condition.Field.FEDORA_ID, "asc");

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
        LOGGER.debug("Finished indexMembership for transaction {}", txId);
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

    /**
     * Parse the inputstream from a Rdf resource to a RDFstream.
     *
     * @param fedoraIdentifier the resource identifier.
     * @param inputStream the inputstream.
     * @return an RdfStream of the resource triples.
     */
    private static RdfStream parseRdf(final FedoraId fedoraIdentifier, final InputStream inputStream) {
        final Model model = createDefaultModel();
        RDFDataMgr.read(model, inputStream, getRdfFormat().getLang());
        final FedoraId topic = (fedoraIdentifier.isDescription() ? fedoraIdentifier.asBaseId() : fedoraIdentifier);
        return DefaultRdfStream.fromModel(createURI(topic.getFullId()), model);
    }
}
