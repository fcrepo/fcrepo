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

import edu.wisc.library.ocfl.api.OcflRepository;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
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

        final var txId = UUID.randomUUID().toString();

        try {
            LOGGER.debug("Reading object ids...");

            try (final var ocflIds = ocflRepository.listObjectIds()) {
                ocflIds.forEach(ocflId -> {
                    LOGGER.debug("Reading {}", ocflId);
                    try (var session = objectSessionFactory.newSession(ocflId)) {
                        indexOcflObject(ocflId, txId, session);
                    } catch (Exception e) {
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
            LOGGER.info("Index rebuild complete");
        } catch (RuntimeException e) {
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

    /**
     * Executes the closure, capturing all exceptions, and logging them as errors.
     *
     * @param failureMessage what to print if the closure fails
     * @param callable closure to execute
     */
    private void execQuietly(final String failureMessage, final Callable<Void> callable) {
        try {
            callable.call();
        } catch (Exception e) {
            LOGGER.error(failureMessage, e);
        }
    }

}
