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

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import org.fcrepo.common.metrics.MetricsHelper;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

/**
 * PersistentStorageSession wrapper for collecting metrics
 *
 * @author pwinckles
 */
public class OcflPersistentStorageSessionMetrics implements PersistentStorageSession {

    private static final String METRIC_NAME = "fcrepo.storage.ocfl.session";
    private static final String OPERATION = "operation";
    private static final Timer persistTimer = Metrics.timer(METRIC_NAME, OPERATION, "persist");
    private static final Timer getHeadersTimer = Metrics.timer(METRIC_NAME, OPERATION, "getHeaders");
    private static final Timer getTriplesTimer = Metrics.timer(METRIC_NAME, OPERATION, "getTriples");
    private static final Timer listVersionsTimer = Metrics.timer(METRIC_NAME, OPERATION, "listVersions");
    private static final Timer getContentTimer = Metrics.timer(METRIC_NAME, OPERATION, "getContent");
    private static final Timer prepareTimer = Metrics.timer(METRIC_NAME, OPERATION, "prepare");
    private static final Timer commitTimer = Metrics.timer(METRIC_NAME, OPERATION, "commit");
    private static final Timer rollbackTimer = Metrics.timer(METRIC_NAME, OPERATION, "rollback");

    private final PersistentStorageSession delegate;

    public OcflPersistentStorageSessionMetrics(final PersistentStorageSession delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public void persist(final ResourceOperation operation) throws PersistentStorageException {
        persistTimer.record(() -> {
            delegate.persist(operation);
        });
    }

    @Override
    public ResourceHeaders getHeaders(final FedoraId identifier, final Instant version)
            throws PersistentStorageException {
        return MetricsHelper.time(getHeadersTimer, () -> {
            return delegate.getHeaders(identifier, version);
        });
    }

    @Override
    public RdfStream getTriples(final FedoraId identifier, final Instant version) throws PersistentStorageException {
        return MetricsHelper.time(getTriplesTimer, () -> {
            return delegate.getTriples(identifier, version);
        });
    }

    @Override
    public InputStream getBinaryContent(final FedoraId identifier, final Instant version)
            throws PersistentStorageException {
        return MetricsHelper.time(getContentTimer, () -> {
            return delegate.getBinaryContent(identifier, version);
        });
    }

    @Override
    public List<Instant> listVersions(final FedoraId identifier) throws PersistentStorageException {
        return MetricsHelper.time(listVersionsTimer, () -> {
            return delegate.listVersions(identifier);
        });
    }

    @Override
    public void prepare() throws PersistentStorageException {
        prepareTimer.record(delegate::prepare);
    }

    @Override
    public void commit() throws PersistentStorageException {
        commitTimer.record(delegate::commit);
    }

    @Override
    public void rollback() throws PersistentStorageException {
        rollbackTimer.record(delegate::rollback);
    }

}
