/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.persistence.ocfl.impl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

/**
 * Wrapper for FedoraToOcflObjectIndex that adds metrics
 *
 * @author pwinckles
 */
@Component("ocflIndex")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class FedoraToOcflObjectIndexMetrics implements FedoraToOcflObjectIndex {

    private static final String METRIC_NAME = "fcrepo.db";
    private static final String DB = "db";
    private static final String OCFL = "ocfl";
    private static final String OPERATION = "operation";

    private static final Timer getMappingTimer = Metrics.timer(METRIC_NAME,
            DB, OCFL, OPERATION, "getMapping");
    private static final Timer addMappingTimer = Metrics.timer(METRIC_NAME,
            DB, OCFL, OPERATION, "addMapping");
    private static final Timer removeMappingTimer = Metrics.timer(METRIC_NAME,
            DB, OCFL, OPERATION, "removeMapping");
    private static final Timer resetTimer = Metrics.timer(METRIC_NAME,
            DB, OCFL, OPERATION, "reset");
    private static final Timer commitTimer = Metrics.timer(METRIC_NAME,
            DB, OCFL, OPERATION, "commit");
    private static final Timer rollbackTimer = Metrics.timer(METRIC_NAME,
            DB, OCFL, OPERATION, "rollback");
    private static final Timer clearAllTransactionsTimer = Metrics.timer(METRIC_NAME,
            DB, OCFL, OPERATION, "clearAllTransactions");

    @Autowired
    private FedoraToOcflObjectIndex ocflIndexImpl;

    @Override
    public FedoraOcflMapping getMapping(final Transaction session, final FedoraId fedoraResourceIdentifier)
            throws FedoraOcflMappingNotFoundException {
        final var stopwatch = Timer.start();
        try  {
            return ocflIndexImpl.getMapping(session, fedoraResourceIdentifier);
        } finally {
            stopwatch.stop(getMappingTimer);
        }
    }

    @Override
    public FedoraOcflMapping addMapping(final Transaction session,
                                        final FedoraId fedoraResourceIdentifier,
                                        final FedoraId fedoraRootObjectIdentifier,
                                        final String ocflObjectId) {
        return MetricsHelper.time(addMappingTimer, () -> {
            return ocflIndexImpl.addMapping(session, fedoraResourceIdentifier,
                    fedoraRootObjectIdentifier, ocflObjectId);
        });
    }

    @Override
    public void removeMapping(final Transaction session, final FedoraId fedoraResourceIdentifier) {
        removeMappingTimer.record(() -> {
            ocflIndexImpl.removeMapping(session, fedoraResourceIdentifier);
        });
    }

    @Override
    public void reset() {
        resetTimer.record(() -> {
            ocflIndexImpl.reset();
        });
    }

    @Override
    public void commit(final Transaction session) {
        commitTimer.record(() -> {
            ocflIndexImpl.commit(session);
        });
    }

    @Override
    public void rollback(final Transaction session) {
        rollbackTimer.record(() -> {
            ocflIndexImpl.rollback(session);
        });
    }

    @Override
    public void clearAllTransactions() {
        clearAllTransactionsTimer.record(() -> {
            ocflIndexImpl.clearAllTransactions();
        });
    }

}
