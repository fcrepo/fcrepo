/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.getRdfFormat;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import io.ocfl.api.OcflRepository;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.ObjectExistsInOcflIndexException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.validation.ObjectValidator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

/**
 * Service that does the reindexing for one OCFL object.
 * @author whikloj
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ReindexService {

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Inject
    private OcflObjectSessionFactory ocflObjectSessionFactory;

    @Autowired
    @Qualifier("ocflIndex")
    private FedoraToOcflObjectIndex ocflIndex;

    @Autowired
    private OcflRepository ocflRepository;

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

    @Inject
    private ObjectValidator objectValidator;

    @Inject
    private FedoraPropsConfig config;

    @Inject
    private ResourceFactory resourceFactory;

    private static final Logger LOGGER = getLogger(ReindexService.class);

    private int membershipPageSize = 500;

    @Inject
    private RepositoryInitializationStatus initializationStatus;

    private long validatingCacheDurationMs = 0;
    private static final AtomicLong parseRdfDurationNs = new AtomicLong(0);
    private static final AtomicLong containmentDurationNs = new AtomicLong(0);
    private static final AtomicLong ocflIndexAddMappingDurationNs = new AtomicLong(0);
    private static final AtomicLong searchIndexUpdateDurationNs = new AtomicLong(0);

    public void indexOcflObject(final Transaction tx, final String ocflId) {
        LOGGER.error("Indexing ocflId {} in transaction {}", ocflId, tx.getId());

        ocflRepository.invalidateCache(ocflId);
        final var start2 = System.nanoTime();
        if (config.isRebuildValidation()) {
            objectValidator.validate(ocflId, config.isRebuildFixityCheck());
        }
        validatingCacheDurationMs += (System.nanoTime() - start2);
        LOGGER.error("Validated OCFL object in {} ms",
                validatingCacheDurationMs / 1_000_000);

        try (final var session = ocflObjectSessionFactory.newSession(ocflId)) {
            final var rootId = new AtomicReference<FedoraId>();
            final var fedoraIds = new ArrayList<FedoraId>();
            final var headersList = new ArrayList<ResourceHeaders>();
            final var rdfTypeMap = new HashMap<FedoraId, List<URI>>();

            session.invalidateCache(ocflId);
            session.streamResourceHeaders().forEach(storageHeaders -> {
                final var headers = new ResourceHeadersAdapter(storageHeaders);

                final var fedoraId = headers.getId();

                // Only check for skip entries when running pre-startup indexing process, live indexing should proceed
                if (!initializationStatus.isInitializationComplete()) {
                    try {
                        ocflIndex.getMapping(tx, fedoraId);
                        // We got the mapping, so we can skip this resource.
                        throw new ObjectExistsInOcflIndexException(
                                String.format("Skipping indexing of %s in transaction %s, because" +
                                        " it already exists in the index.", fedoraId, tx.getId())
                        );
                    } catch (FedoraOcflMappingNotFoundException e) {
                        LOGGER.debug("Indexing object {} in transaction {}, because it does not yet exist in the " +
                                "index.", fedoraId, tx.getId());
                    }
                }

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
                            final var startRdf = System.nanoTime();
                            final Optional<InputStream> content = session.readContent(fedoraId.getFullId())
                                    .getContentStream();
                            if (content.isPresent()) {
                                try (final var stream = content.get()) {
                                    RdfStream rdf = parseRdf(fedoraId, stream);
                                    parseRdfDurationNs.addAndGet(System.nanoTime() - startRdf);
                                    LOGGER.error("RDF parse in {} ms", parseRdfDurationNs.get() / 1_000_000);
                                    rdf = (RdfStream) rdf.peek(t -> {
                                        LOGGER.error("Peeking at triple with predicate {}", t.getPredicate());
                                        if (t.predicateMatches(type.asNode())) {
                                            LOGGER.error("Adding type {} for {}", t.getObject(), fedoraId);
                                            rdfTypeMap.computeIfAbsent(fedoraId, k -> new ArrayList<>())
                                                    .add(URI.create(t.getObject().toString()));
                                        }
                                    });
                                    this.referenceService.updateReferences(tx, fedoraId, null, rdf);
                                } catch (final IOException e) {
                                    LOGGER.warn("Content stream for {} closed prematurely, inbound references skipped.",
                                            fedoraId.getFullId());
                                    throw new RepositoryRuntimeException(e.getMessage(), e);
                                }
                            }
                        }

                        final var startContainment = System.nanoTime();
                        this.containmentIndex.addContainedBy(tx, parentId, fedoraId, created, null);
                        containmentDurationNs.addAndGet(System.nanoTime() - startContainment);
                        LOGGER.error("Containment index update in {} ms",
                                containmentDurationNs.get() / 1_000_000);
                        headersList.add(headers.asKernelHeaders());
                    } else {
                        final var deleted = headers.getLastModifiedDate();
                        final var startContainment = System.nanoTime();
                        this.containmentIndex.addContainedBy(tx, parentId, fedoraId, created, deleted);
                        containmentDurationNs.addAndGet(System.nanoTime() - startContainment);
                        LOGGER.error("Containment deleted index update in {} ms",
                                containmentDurationNs.get() / 1_000_000);
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

            final long start4 = System.nanoTime();
            fedoraIds.forEach(fedoraIdentifier -> {
                final var rootFedoraIdentifier = rootId.get();
                ocflIndex.addMapping(tx, fedoraIdentifier, rootFedoraIdentifier, ocflId);
                LOGGER.debug("Rebuilt fedora-to-ocfl object index entry for {}", fedoraIdentifier);
            });
            ocflIndexAddMappingDurationNs.addAndGet(System.nanoTime() - start4);
            LOGGER.error("Rebuilt ocflIndex in {} ms",
                    ocflIndexAddMappingDurationNs.get() / 1_000_000);

            final long start5 = System.nanoTime();
            headersList.forEach(headers -> {
                try {
                    // Get user RDF types from map and combine with system types
                    final var rdfTypes = rdfTypeMap.getOrDefault(headers.getId(), new ArrayList<>());
                    rdfTypes.addAll(resourceFactory.getResource(tx, headers.getId(), headers).getSystemTypes(false));
                    searchIndex.addUpdateIndex(tx, headers, rdfTypes);
                    LOGGER.error("Rebuilt searchIndex for {}", headers.getId());
                } catch (PathNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
            LOGGER.error("RDF type keys: {}", rdfTypeMap.keySet());
            searchIndexUpdateDurationNs.addAndGet(System.nanoTime() - start5);
            LOGGER.error("Rebuilt searchIndex in {} ms",
                    searchIndexUpdateDurationNs.get() / 1_000_000);
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
     * Index all membership properties by querying for Direct and Indirect containers, and then
     * trying population of the membership index for each one
     * @param transaction the transaction id.
     */
    public void indexMembership(final Transaction transaction) {
        indexContainerType(transaction, RdfLexicon.DIRECT_CONTAINER);
        indexContainerType(transaction, RdfLexicon.INDIRECT_CONTAINER);
    }

    private void indexContainerType(final Transaction transaction, final Resource containerType) {
        LOGGER.debug("Starting indexMembership for transaction {}", transaction);
        final var fields = List.of(Condition.Field.FEDORA_ID);
        final var conditions = List.of(Condition.fromEnums(Condition.Field.RDF_TYPE, Condition.Operator.EQ,
                containerType.getURI()));
        int offset = 0;

        try {
            int numResults;
            do {
                final var params = new SearchParameters(fields, conditions, membershipPageSize,
                        offset, Condition.Field.FEDORA_ID, "asc", false);

                final var searchResult = searchIndex.doSearch(params);
                final var resultList = searchResult.getItems();
                numResults = resultList.size();

                resultList.stream()
                        .map(entry -> FedoraId.create((String) entry.get(Condition.Field.FEDORA_ID.toString())))
                        .forEach(containerId -> membershipService.populateMembershipHistory(transaction, containerId));

                // Results are paged, so step through pages until we reach the last one
                offset += membershipPageSize;
            } while (numResults == membershipPageSize);

        } catch (final InvalidQueryException e) {
            throw new RepositoryRuntimeException("Failed to repopulate membership history", e);
        }
        LOGGER.debug("Finished indexMembership for transaction {}", transaction);
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
