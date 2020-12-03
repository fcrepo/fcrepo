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

import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import edu.wisc.library.ocfl.api.OcflRepository;

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

    @Autowired
    @Qualifier("ocflIndex")
    private FedoraToOcflObjectIndex ocflIndex;

    @Autowired
    @Qualifier("containmentIndex")
    private ContainmentIndex containmentIndex;

    @Inject
    private OcflRepository ocflRepository;

    @Inject
    private ReindexService reindexService;

    @Inject
    private OcflPropsConfig ocflPropsConfig;

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

        reindexService.reset();

        final ReindexManager reindexManager = new ReindexManager(ocflRepository.listObjectIds(), reindexService,
                ocflPropsConfig);

        LOGGER.debug("Reading object ids...");
        final var startTime = Instant.now();
        try {
            reindexManager.start();
            reindexManager.commit();
            LOGGER.info("Reindexing complete.");
        } catch (final InterruptedException e) {
            reindexManager.rollback();
            throw new RuntimeException(e);
        } finally {
            reindexManager.shutdown();
        }
        final var endTime = Instant.now();
        final var count = reindexManager.getCompletedCount();
        final var errors = reindexManager.getErrorCount();
        LOGGER.info("Index rebuild completed {} objects successfully and {} objects had errors in {} ", count, errors,
                getDurationMessage(Duration.between(startTime, endTime)));
    }

    private boolean shouldRebuild() {
        final var repoRoot = getRepoRootMapping();

        if (repoRoot == null) {
            return true;
        }

        return !repoContainsRootObject(repoRoot);
    }

    private String getRepoRootMapping() {
        try {
            return ocflIndex.getMapping(null, FedoraId.getRepositoryRootId()).getOcflObjectId();
        } catch (final FedoraOcflMappingNotFoundException e) {
            return null;
        }
    }

    private boolean repoContainsRootObject(final String id) {
        return ocflRepository.containsObject(id);
    }

    private String getDurationMessage(final Duration duration) {
        String message = String.format("%d seconds", duration.toSecondsPart());
        if (duration.getSeconds() > 60) {
            message = String.format("%d mins, ", duration.toMinutesPart()) + message;
        }
        if (duration.getSeconds() > 3600) {
            message = String.format("%d hours, ", duration.toHoursPart()) + message;
        }
        return message;
    }
}
