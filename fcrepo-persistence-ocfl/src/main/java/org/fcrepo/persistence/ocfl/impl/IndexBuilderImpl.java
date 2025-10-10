/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import io.ocfl.api.OcflRepository;
import jakarta.inject.Inject;
import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        final String logMessage;
        if (fedoraPropsConfig.isRebuildContinue()) {
            logMessage = "Initiating partial index rebuild. This will add missing objects to the index.";
        } else {
            logMessage = "Initiating index rebuild.";
        }
        LOGGER.info(logMessage + " This may take a while. Progress will be logged periodically.");

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
            final var skipped = reindexManager.getSkippedCount();
            if (fedoraPropsConfig.isRebuildContinue()) {
                LOGGER.info(
                    "Index rebuild completed {} objects successfully, {} objects skipped and {} objects had errors " +
                    "in {} ", count, skipped, errors, getDurationMessage(Duration.between(startTime, endTime))
                );
            } else {
                LOGGER.info(
                    "Index rebuild completed {} objects successfully and {} objects had errors in {} ",
                    count, errors, getDurationMessage(Duration.between(startTime, endTime))
                );
            }
        }
    }

    private boolean shouldRebuild() {
        return fedoraPropsConfig.isRebuildContinue();
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
