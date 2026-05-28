/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * ContainmentIndex wrapper for adding metrics
 *
 * @author pwinckles
 */
@Component("containmentIndex")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ContainmentIndexMetrics implements ContainmentIndex {

    private static final String METRIC_NAME = "fcrepo.db";
    private static final String DB = "db";
    private static final String CONTAINMENT = "containment";
    private static final String OPERATION = "operation";

    private static final Timer getContainsTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "getContains");
    private static final Timer getContainsDeletedTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "getContainsDeleted");
    private static final Timer getContainsByTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "getContainsBy");
    private static final Timer removeContainedByTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "removeContainedBy");
    private static final Timer removeResourceTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "removeResource");
    private static final Timer purgeResourceTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "purgeResource");
    private static final Timer addContainedByTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "addContainedBy");
    private static final Timer commitTransactionTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "commitTransaction");
    private static final Timer rollbackTransactionTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "rollbackTransaction");
    private static final Timer resourceExistsTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "resourceExists");
    private static final Timer getContainerIdByPathTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "getContainerIdByPath");
    private static final Timer resetTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "reset");
    private static final Timer clearAllTransactionsTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "clearAllTransactions");
    private static final Timer hasResourcesStartingWithTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "hasResourcesStartingWith");
    private static final Timer containmentLastUpdateTimer = Metrics.timer(METRIC_NAME, DB, CONTAINMENT, OPERATION,
            "containmentLastUpdated");

    @Autowired
    @Qualifier("containmentIndexImpl")
    private ContainmentIndex containmentIndexImpl;

    @Override
    public Stream<String> getContains(final Transaction tx, final FedoraId fedoraId) {
        return MetricsHelper.time(getContainsTimer, () -> {
            return containmentIndexImpl.getContains(tx, fedoraId);
        });
    }

    @Override
    public Stream<String> getContainsDeleted(final Transaction tx, final FedoraId fedoraId) {
        return MetricsHelper.time(getContainsDeletedTimer, () -> {
            return containmentIndexImpl.getContainsDeleted(tx, fedoraId);
        });
    }

    @Override
    public String getContainedBy(final Transaction tx, final FedoraId resource) {
        return MetricsHelper.time(getContainsByTimer, () -> {
            return containmentIndexImpl.getContainedBy(tx, resource);
        });
    }

    @Override
    public void removeContainedBy(final Transaction tx, final FedoraId parent, final FedoraId child) {
        removeContainedByTimer.record(() -> {
            containmentIndexImpl.removeContainedBy(tx, parent, child);
        });
    }

    @Override
    public void removeResource(final Transaction tx, final FedoraId resource) {
        removeResourceTimer.record(() -> {
            containmentIndexImpl.removeResource(tx, resource);
        });
    }

    @Override
    public void purgeResource(final Transaction tx, final FedoraId resource) {
        purgeResourceTimer.record(() -> {
            containmentIndexImpl.purgeResource(tx, resource);
        });
    }

    @Override
    public void addContainedBy(final Transaction tx, final FedoraId parent, final FedoraId child) {
        addContainedByTimer.record(() -> {
            containmentIndexImpl.addContainedBy(tx, parent, child);
        });
    }

    @Override
    public void addContainedBy(final Transaction tx, final FedoraId parent, final FedoraId child,
                               final Instant startTime, final Instant endTime) {
        addContainedByTimer.record(() -> containmentIndexImpl.addContainedBy(tx, parent, child, startTime, endTime));
    }

    @Override
    public void commitTransaction(final Transaction tx) {
        commitTransactionTimer.record(() -> {
            containmentIndexImpl.commitTransaction(tx);
        });
    }

    @Override
    public void rollbackTransaction(final Transaction tx) {
        rollbackTransactionTimer.record(() -> {
            containmentIndexImpl.rollbackTransaction(tx);
        });
    }

    @Override
    public void clearAllTransactions() {
        clearAllTransactionsTimer.record(() -> {
            containmentIndexImpl.clearAllTransactions();
        });
    }

    @Override
    public boolean resourceExists(final Transaction tx, final FedoraId fedoraId, final boolean includeDeleted) {
        return MetricsHelper.time(resourceExistsTimer, () -> {
            return containmentIndexImpl.resourceExists(tx, fedoraId, includeDeleted);
        });
    }

    @Override
    public FedoraId getContainerIdByPath(final Transaction tx, final FedoraId fedoraId, final boolean checkDeleted) {
        return MetricsHelper.time(getContainerIdByPathTimer, () -> {
            return containmentIndexImpl.getContainerIdByPath(tx, fedoraId, checkDeleted);
        });
    }

    @Override
    public void reset() {
        resetTimer.record(() -> {
            containmentIndexImpl.reset();
        });
    }

    @Override
    public boolean hasResourcesStartingWith(final Transaction tx, final FedoraId fedoraId) {
        return MetricsHelper.time(hasResourcesStartingWithTimer, () ->
                containmentIndexImpl.hasResourcesStartingWith(tx, fedoraId));
    }

    @Override
    public Instant containmentLastUpdated(final Transaction tx, final FedoraId fedoraId) {
        return MetricsHelper.time(containmentLastUpdateTimer, () ->
                containmentIndexImpl.containmentLastUpdated(tx, fedoraId));
    }
}
