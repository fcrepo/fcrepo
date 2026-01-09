/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.search.impl;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

/**
 * SearchIndex wrapper for collecting metrics
 *
 * @author pwinckles
 */
@Component("searchIndex")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
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
    private static final Timer commitTransactionTimer = Metrics.timer(METRIC_NAME,
            DB, SEARCH, OPERATION, "commitTransaction");
    private static final Timer rollbackTransactionTimer = Metrics.timer(METRIC_NAME,
            DB, SEARCH, OPERATION, "rollbackTransaction");
    private static final Timer clearAllTransactionsTimer = Metrics.timer(METRIC_NAME,
            DB, SEARCH, OPERATION, "clearAllTransactions");

    @Autowired
    @Qualifier("searchIndexImpl")
    private SearchIndex searchIndexImpl;

    @Override
    public void addUpdateIndex(final Transaction transaction, final ResourceHeaders resourceHeaders) {
        addUpdateIndexTimer.record(() -> {
            searchIndexImpl.addUpdateIndex(transaction, resourceHeaders);
        });
    }

    @Override
    public void addUpdateIndex(final Transaction transaction,
                               final ResourceHeaders resourceHeaders,
                               final List<URI> rdfTypes) {
        addUpdateIndexTimer.record(() -> {
            searchIndexImpl.addUpdateIndex(transaction, resourceHeaders, rdfTypes);
        });
    }

    @Override
    public void removeFromIndex(final Transaction transaction, final FedoraId fedoraId) {
        removeFromIndexTimer.record(() -> {
            searchIndexImpl.removeFromIndex(transaction, fedoraId);
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

    @Override
    public void commitTransaction(final Transaction tx) {
        commitTransactionTimer.record(() -> {
            searchIndexImpl.commitTransaction(tx);
        });
    }

    @Override
    public void rollbackTransaction(final Transaction tx) {
        rollbackTransactionTimer.record(() -> {
            searchIndexImpl.rollbackTransaction(tx);
        });
    }

    @Override
    public void clearAllTransactions() {
        clearAllTransactionsTimer.record(() -> {
            searchIndexImpl.clearAllTransactions();
        });
    }
}
