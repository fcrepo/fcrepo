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

package org.fcrepo.kernel.impl.services;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * ReferenceService wrapper for collecting metrics
 *
 * @author pwinckles
 */
@Component("referenceService")
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

    @Autowired
    @Qualifier("referenceServiceImpl")
    private ReferenceService referenceServiceImpl;

    @Override
    public RdfStream getInboundReferences(final String txId, final FedoraResource resource) {
        return MetricsHelper.time(getInboundReferences, () -> {
            return referenceServiceImpl.getInboundReferences(txId, resource);
        });
    }

    @Override
    public void deleteAllReferences(final String txId, final FedoraId resourceId) {
        deleteAllReferencesTimer.record(() -> {
            referenceServiceImpl.deleteAllReferences(txId, resourceId);
        });
    }

    @Override
    public void updateReferences(final String txId,
                                 final FedoraId resourceId,
                                 final String userPrincipal,
                                 final RdfStream rdfStream) {
        updateReferencesTimer.record(() -> {
            referenceServiceImpl.updateReferences(txId, resourceId, userPrincipal, rdfStream);
        });
    }

    @Override
    public void commitTransaction(final String txId) {
        commitTransactionTimer.record(() -> {
            referenceServiceImpl.commitTransaction(txId);
        });
    }

    @Override
    public void rollbackTransaction(final String txId) {
        rollbackTransactionTimer.record(() -> {
            referenceServiceImpl.rollbackTransaction(txId);
        });
    }

    @Override
    public void reset() {
        resetTimer.record(() -> {
            referenceServiceImpl.reset();
        });
    }

}
