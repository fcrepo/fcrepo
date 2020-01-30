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

import java.util.stream.Stream;

import static org.fcrepo.persistence.common.ResourceHeaderSerializationUtils.deserializeHeaders;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.getSidecarSubpath;

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

    /**
     * Default constructor
     */
    public FedoraToOCFLObjectIndexUtilImpl() {
    }


    @Override
    public void rebuild() {

        final Stream<String> ocflIds = ocflRepository.listObjectIds();
        LOGGER.info("Initiating index rebuild.");
        fedoraToOCFLObjectIndex.reset();
        LOGGER.debug("Reading object ids...");
        ocflIds.forEach(ocflId -> {
            try {
                LOGGER.debug("Reading {}", ocflId);
                final var objSession = objectSessionFactory.create(ocflId, null);
                final var sidecarSubpath = getSidecarSubpath(ocflId);
                final var headers = deserializeHeaders(objSession.read(sidecarSubpath));
                final var fedoraIdentifier = headers.getId();
                fedoraToOCFLObjectIndex.addMapping(fedoraIdentifier, fedoraIdentifier, ocflId);
                LOGGER.debug("Index entry created for {}", ocflId);
            } catch (final PersistentStorageException e) {
                throw new RepositoryRuntimeException("Failed to rebuild fedora-to-ocfl index: " + e.getMessage(), e);
            }
        });
        LOGGER.info("Index rebuild complete");
    }
}
