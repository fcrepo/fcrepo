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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Wrapper for FedoraToOcflObjectIndex that adds metrics
 *
 * @author pwinckles
 */
@Component("ocflIndex")
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

    @Autowired
    private FedoraToOcflObjectIndex ocflIndexImpl;

    @Override
    public FedoraOcflMapping getMapping(final String sessionId, final FedoraId fedoraResourceIdentifier)
            throws FedoraOcflMappingNotFoundException {
        final var stopwatch = Timer.start();
        try  {
            return ocflIndexImpl.getMapping(sessionId, fedoraResourceIdentifier);
        } finally {
            stopwatch.stop(getMappingTimer);
        }
    }

    @Override
    public FedoraOcflMapping addMapping(final String sessionId,
                                        final FedoraId fedoraResourceIdentifier,
                                        final FedoraId fedoraRootObjectIdentifier,
                                        final String ocflObjectId) {
        return MetricsHelper.time(addMappingTimer, () -> {
            return ocflIndexImpl.addMapping(sessionId, fedoraResourceIdentifier,
                    fedoraRootObjectIdentifier, ocflObjectId);
        });
    }

    @Override
    public void removeMapping(final String sessionId, final FedoraId fedoraResourceIdentifier) {
        removeMappingTimer.record(() -> {
            ocflIndexImpl.removeMapping(sessionId, fedoraResourceIdentifier);
        });
    }

    @Override
    public void reset() {
        resetTimer.record(() -> {
            ocflIndexImpl.reset();
        });
    }

    @Override
    public void commit(final String sessionId) {
        commitTimer.record(() -> {
            ocflIndexImpl.commit(sessionId);
        });
    }

    @Override
    public void rollback(final String sessionId) {
        rollbackTimer.record(() -> {
            ocflIndexImpl.rollback(sessionId);
        });
    }

}
