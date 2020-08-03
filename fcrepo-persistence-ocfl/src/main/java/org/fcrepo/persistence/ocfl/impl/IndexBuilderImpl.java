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
import org.apache.commons.io.FileUtils;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.persistence.ocfl.api.OcflObjectSession;
import org.fcrepo.persistence.ocfl.api.OcflObjectSessionFactory;
import org.fcrepo.search.api.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;

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

    @Value("#{ocflPropsConfig.fedoraOcflStaging}")
    private Path sessionStagingRoot;

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
        final var stagingDir = createStagingDir(txId);

        try {
            LOGGER.debug("Reading object ids...");

            try (final var ocflIds = ocflRepository.listObjectIds()) {
                ocflIds.forEach(ocflId -> {
                    LOGGER.debug("Reading {}", ocflId);
                    try (final var session = objectSessionFactory.create(ocflId, stagingDir)) {
                        indexOcflObject(ocflId, txId, session);
                    }
                });
            }

            containmentIndex.commitTransaction(txId);
            fedoraToOcflObjectIndex.commit(txId);
            LOGGER.info("Index rebuild complete");
        } catch (RuntimeException e) {
            execQuietly("Failed to rollback containment index transaction " + txId, () -> {
                containmentIndex.rollbackTransaction(txId);
                return null;
            });
            execQuietly("Failed to rollback OCFL index transaction " + txId, () -> {
                fedoraToOcflObjectIndex.rollback(txId);
                return null;
            });
            throw e;
        } finally {
            cleanupStaging(stagingDir);
        }
    }

    private void indexOcflObject(final String ocflId, final String txId, final OcflObjectSession session) {
        try (final var subpaths = session.listHeadSubpaths()) {
            final var rootId = new AtomicReference<FedoraId>();
            final var fedoraIds = new ArrayList<FedoraId>();
            final var headersList = new ArrayList<ResourceHeaders>();
            subpaths.forEach(subpath -> {
                if (PersistencePaths.isHeaderFile(subpath)) {
                    //we're only interested in sidecar subpaths
                    try {
                        final var headers = deserializeHeaders(session.read(subpath));
                        final var fedoraId = headers.getId();
                        fedoraIds.add(fedoraId);
                        if (headers.isArchivalGroup() || headers.isObjectRoot()) {
                            rootId.set(headers.getId());
                        }

                        if (!fedoraId.isRepositoryRoot()) {
                            var parentId = headers.getParent();

                            if (parentId == null) {
                                if (headers.isObjectRoot()) {
                                    parentId = FedoraId.getRepositoryRootId();
                                }
                            }

                            if (parentId != null) {
                                this.containmentIndex.addContainedBy(txId, parentId,
                                        headers.getId());
                                headersList.add(headers);
                            }


                        }
                    } catch (PersistentStorageException e) {
                        throw new RepositoryRuntimeException(format("fedora-to-ocfl index rebuild failed: %s",
                                e.getMessage()), e);
                    }
                }
            });

            // if a resource is not an AG then there should only be a single resource per OCFL object
            if (fedoraIds.size() == 1 && rootId.get() == null) {
                rootId.set(fedoraIds.get(0));
            }

            fedoraIds.forEach(fedoraIdentifier -> {
                var rootFedoraIdentifier = rootId.get();
                if (rootFedoraIdentifier == null) {
                    rootFedoraIdentifier = fedoraIdentifier;
                }
                fedoraToOcflObjectIndex.addMapping(txId, fedoraIdentifier, rootFedoraIdentifier, ocflId);
                LOGGER.debug("Rebuilt fedora-to-ocfl object index entry for {}", fedoraIdentifier);
            });

            headersList.forEach(headers -> {
                if (!headers.isDeleted()) {
                    searchIndex.addUpdateIndex(txId, headers);
                    LOGGER.debug("Rebuilt searchIndex for {}", headers.getId());
                }
            });

        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException("Failed to rebuild fedora-to-ocfl index: " +
                    e.getMessage(), e);
        }
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

    private Path createStagingDir(final String txId) {
        try {
            return Files.createDirectories(sessionStagingRoot.resolve(txId));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void cleanupStaging(final Path stagingDir) {
        if (!FileUtils.deleteQuietly(stagingDir.toFile())) {
            LOGGER.warn("Failed to cleanup staging directory: {}", stagingDir);
        }
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
