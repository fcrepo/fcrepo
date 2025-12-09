/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.services;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

/**
 * ReferenceService wrapper for collecting metrics
 *
 * @author pwinckles
 */
@Component("referenceService")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ReferenceServiceMetrics implements ReferenceService {

    private static final String METRIC_NAME = "fcrepo.db";
    private static final String DB = "db";
    private static final String REFERENCE = "reference";
    private static final String OPERATION = "operation";

    private static final Timer getInboundReferences = Metrics.timer(METRIC_NAME,
            DB, REFERENCE, OPERATION, "getInboundReferences");
    private static final Timer deleteAllReferencesTimer = Metrics.timer(METRIC_NAME,
            DB, REFERENCE, OPERATION, "deleteAllReferences");
    private static final Timer updateReferencesTimer = Metrics.timer(METRIC_NAME,
            DB, REFERENCE, OPERATION, "updateReferences");
    private static final Timer commitTransactionTimer = Metrics.timer(METRIC_NAME,
            DB, REFERENCE, OPERATION, "commitTransaction");
    private static final Timer rollbackTransactionTimer = Metrics.timer(METRIC_NAME,
            DB, REFERENCE, OPERATION, "rollbackTransaction");
    private static final Timer resetTimer = Metrics.timer(METRIC_NAME,
            DB, REFERENCE, OPERATION, "reset");
    private static final Timer clearAllTransactionsTimer = Metrics.timer(METRIC_NAME,
            DB, REFERENCE, OPERATION, "clearAllTransactions");

    @Autowired
    @Qualifier("referenceServiceImpl")
    private ReferenceService referenceServiceImpl;

    @Override
    public RdfStream getInboundReferences(final Transaction tx, final FedoraResource resource) {
        return MetricsHelper.time(getInboundReferences, () -> {
            return referenceServiceImpl.getInboundReferences(tx, resource);
        });
    }

    @Override
    public void deleteAllReferences(final Transaction tx, final FedoraId resourceId) {
        deleteAllReferencesTimer.record(() -> {
            referenceServiceImpl.deleteAllReferences(tx, resourceId);
        });
    }

    @Override
    public void updateReferences(final Transaction tx,
                                 final FedoraId resourceId,
                                 final String userPrincipal,
                                 final RdfStream rdfStream) {
        updateReferencesTimer.record(() -> {
            referenceServiceImpl.updateReferences(tx, resourceId, userPrincipal, rdfStream);
        });
    }

    @Override
    public void commitTransaction(final Transaction tx) {
        commitTransactionTimer.record(() -> {
            referenceServiceImpl.commitTransaction(tx);
        });
    }

    @Override
    public void rollbackTransaction(final Transaction tx) {
        rollbackTransactionTimer.record(() -> {
            referenceServiceImpl.rollbackTransaction(tx);
        });
    }

    @Override
    public void clearAllTransactions() {
        clearAllTransactionsTimer.record(() -> {
            referenceServiceImpl.clearAllTransactions();
        });
    }

    @Override
    public void reset() {
        resetTimer.record(() -> {
            referenceServiceImpl.reset();
        });
    }

}
