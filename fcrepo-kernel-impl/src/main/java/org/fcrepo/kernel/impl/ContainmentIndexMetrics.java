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

package org.fcrepo.kernel.impl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * ContainmentIndex wrapper for adding metrics
 *
 * @author pwinckles
 */
@Component("containmentIndex")
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
    private static final Timer hasResourcesStartingWithTimer = Metrics.timer(METRIC_NAME,
            DB, CONTAINMENT, OPERATION, "hasResourcesStartingWith");
    private static final Timer containmentLastUpdateTimer = Metrics.timer(METRIC_NAME, DB, CONTAINMENT, OPERATION,
            "containmentLastUpdated");

    @Autowired
    @Qualifier("containmentIndexImpl")
    private ContainmentIndex containmentIndexImpl;

    @Override
    public Stream<String> getContains(final String txId, final FedoraId fedoraId) {
        return MetricsHelper.time(getContainsTimer, () -> {
            return containmentIndexImpl.getContains(txId, fedoraId);
        });
    }

    @Override
    public Stream<String> getContainsDeleted(final String txId, final FedoraId fedoraId) {
        return MetricsHelper.time(getContainsDeletedTimer, () -> {
            return containmentIndexImpl.getContainsDeleted(txId, fedoraId);
        });
    }

    @Override
    public String getContainedBy(final String txID, final FedoraId resource) {
        return MetricsHelper.time(getContainsByTimer, () -> {
            return containmentIndexImpl.getContainedBy(txID, resource);
        });
    }

    @Override
    public void removeContainedBy(final String txID, final FedoraId parent, final FedoraId child) {
        removeContainedByTimer.record(() -> {
            containmentIndexImpl.removeContainedBy(txID, parent, child);
        });
    }

    @Override
    public void removeResource(final String txID, final FedoraId resource) {
        removeResourceTimer.record(() -> {
            containmentIndexImpl.removeResource(txID, resource);
        });
    }

    @Override
    public void purgeResource(final String txID, final FedoraId resource) {
        purgeResourceTimer.record(() -> {
            containmentIndexImpl.purgeResource(txID, resource);
        });
    }

    @Override
    public void addContainedBy(final String txID, final FedoraId parent, final FedoraId child) {
        addContainedByTimer.record(() -> {
            containmentIndexImpl.addContainedBy(txID, parent, child);
        });
    }

    @Override
    public void addContainedBy(final String txId, final FedoraId parent, final FedoraId child,
                               final Instant startTime, final Instant endTime) {
        addContainedByTimer.record(() -> containmentIndexImpl.addContainedBy(txId, parent, child, startTime, endTime));
    }

    @Override
    public void commitTransaction(final String txId) {
        commitTransactionTimer.record(() -> {
            containmentIndexImpl.commitTransaction(txId);
        });
    }

    @Override
    public void rollbackTransaction(final String txId) {
        rollbackTransactionTimer.record(() -> {
            containmentIndexImpl.rollbackTransaction(txId);
        });
    }

    @Override
    public boolean resourceExists(final String txID, final FedoraId fedoraId, final boolean includeDeleted) {
        return MetricsHelper.time(resourceExistsTimer, () -> {
            return containmentIndexImpl.resourceExists(txID, fedoraId, includeDeleted);
        });
    }

    @Override
    public FedoraId getContainerIdByPath(final String txID, final FedoraId fedoraId, final boolean checkDeleted) {
        return MetricsHelper.time(getContainerIdByPathTimer, () -> {
            return containmentIndexImpl.getContainerIdByPath(txID, fedoraId, checkDeleted);
        });
    }

    @Override
    public void reset() {
        resetTimer.record(() -> {
            containmentIndexImpl.reset();
        });
    }

    @Override
    public boolean hasResourcesStartingWith(final String txId, final FedoraId fedoraId) {
        return MetricsHelper.time(hasResourcesStartingWithTimer, () ->
                containmentIndexImpl.hasResourcesStartingWith(txId, fedoraId));
    }

    @Override
    public Instant containmentLastUpdated(final String txId, final FedoraId fedoraId) {
        return MetricsHelper.time(containmentLastUpdateTimer, () ->
                containmentIndexImpl.containmentLastUpdated(txId, fedoraId));
    }
}
