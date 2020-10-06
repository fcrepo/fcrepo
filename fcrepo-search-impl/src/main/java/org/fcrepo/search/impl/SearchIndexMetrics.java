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

package org.fcrepo.search.impl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * SearchIndex wrapper for collecting metrics
 *
 * @author pwinckles
 */
@Component("searchIndex")
public class SearchIndexMetrics implements SearchIndex {

    private static final String METRIC_NAME = "fcrepo.db";
    private static final String DB = "db";
    private static final String SEARCH = "search";
    private static final String OPERATION = "operation";

    private static final Timer addUpdateIndexTimer = Metrics.timer(METRIC_NAME,
            DB, SEARCH, OPERATION, "addUpdateIndex");
    private static final Timer removeFromIndexTimer = Metrics.timer(METRIC_NAME,
            DB, SEARCH, OPERATION, "removeFromIndex");
    private static final Timer doSearchTimer = Metrics.timer(METRIC_NAME,
            DB, SEARCH, OPERATION, "doSearch");
    private static final Timer resetTimer = Metrics.timer(METRIC_NAME,
            DB, SEARCH, OPERATION, "reset");

    @Autowired
    @Qualifier("searchIndexImpl")
    private SearchIndex searchIndexImpl;

    @Override
    public void addUpdateIndex(final ResourceHeaders resourceHeaders) {
        addUpdateIndexTimer.record(() -> {
            searchIndexImpl.addUpdateIndex(resourceHeaders);
        });
    }

    @Override
    public void addUpdateIndex(final String dbTxId, final ResourceHeaders resourceHeaders) {
        addUpdateIndexTimer.record(() -> {
            searchIndexImpl.addUpdateIndex(dbTxId, resourceHeaders);
        });
    }

    @Override
    public void removeFromIndex(final FedoraId fedoraId) {
        removeFromIndexTimer.record(() -> {
            searchIndexImpl.removeFromIndex(fedoraId);
        });
    }

    @Override
    public SearchResult doSearch(final SearchParameters parameters) throws InvalidQueryException {
        final var stopwatch = Timer.start();
        try {
            return searchIndexImpl.doSearch(parameters);
        } finally {
            stopwatch.stop(doSearchTimer);
        }
    }

    @Override
    public void reset() {
        resetTimer.record(() -> {
            searchIndexImpl.reset();
        });
    }

}
