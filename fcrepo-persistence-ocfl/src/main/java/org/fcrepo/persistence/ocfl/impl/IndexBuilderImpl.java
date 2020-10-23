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
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.getRdfFormat;

import edu.wisc.library.ocfl.api.OcflRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * An implementation of {@link IndexBuilder}.  This implementation rebuilds the following indexable state derived
 * from the underlying OCFL directory:
 * 1) the link between a {@link org.fcrepo.kernel.api.identifiers.FedoraId} and an OCFL object identifier
 * 2) the containment relationships between {@link org.fcrepo.kernel.api.identifiers.FedoraId}s
 * 3) the reference relationships between {@link org.fcrepo.kernel.api.identifiers.FedoraId}s
 * 4) the search index
 * 5) the membership relationships for Direct and Indirect containers.
 *
 * @author dbernstein
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class IndexBuilderImpl implements IndexBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexBuilderImpl.class);

    private int membershipPageSize = 500;

    @Inject
    private OcflObjectSessionFactory objectSessionFactory;

    @Autowired
    @Qualifier("ocflIndex")
    private FedoraToOcflObjectIndex ocflIndex;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Autowired
    @Qualifier("searchIndex")
    private SearchIndex searchIndex;

    @Inject
    private OcflRepository ocflRepository;

    @Autowired
    @Qualifier("referenceService")
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

        ocflIndex.reset();
        containmentIndex.reset();
        searchIndex.reset();
        referenceService.reset();
        membershipService.reset();

        final var reindexService = new ReindexService(objectSessionFactory, ocflIndex, containmentIndex, searchIndex,
                referenceService, membershipService, membershipPageSize);
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        final int threads = availableProcessors > 1 ? availableProcessors - 1 : 1;
        final var executor = Executors.newFixedThreadPool(threads);
        final ReindexManager reindexManager = new ReindexManager(executor, reindexService);

        LOGGER.debug("Reading object ids...");
        final var startTime = Instant.now();

        try (final var ocflIds = ocflRepository.listObjectIds()) {
            ocflIds.forEach(reindexManager::submit);
        }

        try {
            reindexManager.awaitCompletion();
            LOGGER.info("Reindexing complete.");
            reindexManager.shutdown();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        final var endTime = Instant.now();
        LOGGER.info("Index rebuild complete in {} milliseconds", Duration.between(startTime, endTime).toMillis());
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
            return ocflIndex.getMapping(null, FedoraId.getRepositoryRootId()) != null;
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
