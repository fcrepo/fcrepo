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
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndexUtil;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

import static org.fcrepo.persistence.api.PersistenceConstants.PERSISTENCE_FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.isSidecarSubpath;

/**
 * An implementation of {@link FedoraToOCFLObjectIndexUtil}
 *
 * @author dbernstein
 * @since 6.0.0
 */
@Component
public class FedoraToOCFLObjectIndexUtilImpl implements FedoraToOCFLObjectIndexUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(FedoraToOCFLObjectIndexUtilImpl.class);

    @Inject
    private OCFLObjectSessionFactory objectSessionFactory;

    @Inject
    private FedoraToOCFLObjectIndex fedoraToOCFLObjectIndex;

    @Inject
    private OcflRepository ocflRepository;

    @Override
    public void rebuild() {

        LOGGER.info("Initiating index rebuild.");
        fedoraToOCFLObjectIndex.reset();
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
                                fedoraIds.add(headers.getId());
                                if (headers.isArchivalGroup()) {
                                    rootId.set(headers.getId());
                                } else if (Objects.equals(headers.getInteractionModel(),
                                        PERSISTENCE_FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
                                    rootId.set(headers.getParent());
                                }
                            } catch (final PersistentStorageException e) {
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
                        fedoraToOCFLObjectIndex.addMapping(fedoraIdentifier, rootId.get(), ocflId);
                        LOGGER.debug("Rebuilt fedora-to-ocfl object index entry for {}", fedoraIdentifier);
                    });

                } catch (final PersistentStorageException e) {
                    throw new RepositoryRuntimeException("Failed to rebuild fedora-to-ocfl index: " +
                            e.getMessage(), e);
                }
            });
        }
        LOGGER.info("Index rebuild complete");
    }
}
