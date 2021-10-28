/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import edu.wisc.library.ocfl.api.OcflRepository;

import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;

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

    @Inject
    private FedoraPropsConfig fedoraPropsConfig;

    @Inject
    private TransactionManager txManager;

    @Inject
    private DbTransactionExecutor dbTransactionExecutor;

    @Override
    public void rebuildIfNecessary() {
        if (shouldRebuild()) {
            rebuild();
        } else {
            LOGGER.debug("No index rebuild necessary");
        }
    }

    private void rebuild() {
        LOGGER.info("Initiating index rebuild. This may take a while. Progress will be logged periodically.");

        reindexService.reset();

        try (var objectIds = ocflRepository.listObjectIds()) {
            final ReindexManager reindexManager = new ReindexManager(objectIds,
                    reindexService, ocflPropsConfig, txManager, dbTransactionExecutor);

            LOGGER.debug("Reading object ids...");
            final var startTime = Instant.now();
            try {
                reindexManager.start();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                reindexManager.shutdown();
            }
            final var endTime = Instant.now();
            final var count = reindexManager.getCompletedCount();
            final var errors = reindexManager.getErrorCount();
            LOGGER.info("Index rebuild completed {} objects successfully and {} objects had errors in {} ",
                    count, errors, getDurationMessage(Duration.between(startTime, endTime)));
        }
    }

    private boolean shouldRebuild() {
        final var repoRoot = getRepoRootMapping();
        if (fedoraPropsConfig.isRebuildOnStart()) {
            return true;
        } else if (repoRoot == null) {
            return true;
        } else {
            return !repoContainsRootObject(repoRoot);
        }
    }

    private String getRepoRootMapping() {
        try {
            return ocflIndex.getMapping(ReadOnlyTransaction.INSTANCE, FedoraId.getRepositoryRootId()).getOcflObjectId();
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
            message = String.format("%d hours, ", duration.getSeconds() / 3600) + message;
        }
        return message;
    }
}
