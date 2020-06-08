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
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.isSidecarSubpath;

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
    private OCFLObjectSessionFactory objectSessionFactory;

    @Inject
    private FedoraToOcflObjectIndex fedoraToOCFLObjectIndex;

    @Inject
    private ContainmentIndex containmentIndex;

    @Inject
    private TransactionManager transactionManager;

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

    @Override
    public void rebuild() {
        LOGGER.info("Initiating index rebuild.");

        fedoraToOCFLObjectIndex.reset();
        containmentIndex.reset();

        final var transaction = transactionManager.create();
        final var txId = transaction.getId();
        LOGGER.debug("Reading object ids...");

        try (final var ocflIds = ocflRepository.listObjectIds()) {
            ocflIds.forEach(ocflId -> {
                LOGGER.debug("Reading {}", ocflId);
                final var objSession = objectSessionFactory.create(ocflId, null);

                //list all the subpaths
                try (final var subpaths = objSession.listHeadSubpaths()) {

                    final var rootId = new AtomicReference<String>();
                    final var fedoraIds = new ArrayList<String>();

                    subpaths.forEach(subpath -> {
                        if (isSidecarSubpath(subpath)) {
                            //we're only interested in sidecar subpaths
                            try {
                                final var headers = deserializeHeaders(objSession.read(subpath));
                                final var fedoraId = FedoraId.create(headers.getId());
                                fedoraIds.add(fedoraId.getFullId());
                                if (headers.isArchivalGroup() || headers.isObjectRoot()) {
                                    rootId.set(headers.getId());
                                }

                                if (!fedoraId.isRepositoryRoot()) {
                                    var parentId = headers.getParent();

                                    if (parentId == null) {
                                        if (headers.isObjectRoot()) {
                                            parentId = FedoraId.getRepositoryRootId().getFullId();
                                        }
                                    }

                                    if (parentId != null) {
                                        this.containmentIndex.addContainedBy(txId, FedoraId.create(parentId),
                                                FedoraId.create(headers.getId()));
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
                        fedoraToOCFLObjectIndex.addMapping(fedoraIdentifier, rootFedoraIdentifier, ocflId);
                        LOGGER.debug("Rebuilt fedora-to-ocfl object index entry for {}", fedoraIdentifier);
                    });

                } catch (final PersistentStorageException e) {
                    throw new RepositoryRuntimeException("Failed to rebuild fedora-to-ocfl index: " +
                            e.getMessage(), e);
                }
            });
        }

        containmentIndex.commitTransaction(transaction);
        LOGGER.info("Index rebuild complete");
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
            return fedoraToOCFLObjectIndex.getMapping(FedoraId.getRepositoryRootId().getFullId()) != null;
        } catch (FedoraOCFLMappingNotFoundException e) {
            return false;
        }
    }

    private boolean repoRootContainmentExists() {
        return containmentIndex.resourceExists(null, FedoraId.getRepositoryRootId());
    }

    private boolean repoContainsObjects() {
        return ocflRepository.listObjectIds().findFirst().isPresent();
    }

}
