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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.fcrepo.config.OcflPropsConfig;
import org.slf4j.Logger;

/**
 * Class to coordinate the index rebuilding tasks.
 * @author whikloj
 * @since 6.0.0
 */
public class ReindexManager {

    private static final Logger LOGGER = getLogger(ReindexManager.class);

    private final String transactionId;

    private final List<ReindexWorker> workers;

    private final Iterator<String> ocflIter;

    private final Stream<String> ocflStream;

    private AtomicInteger completedCount;

    private AtomicInteger errorCount;

    private final ReindexService reindexService;

    private final long batchSize;

    private final boolean failOnError;

    /**
     * Basic constructor
     * @param ids stream of ocfl ids.
     * @param reindexService the reindexing service.
     * @param config OCFL property config object.
     */
    public ReindexManager(final Stream<String> ids, final ReindexService reindexService, final OcflPropsConfig config) {
        this.ocflStream = ids;
        this.ocflIter = ocflStream.iterator();
        this.reindexService = reindexService;
        this.batchSize = config.getReindexBatchSize();
        this.failOnError = config.isReindexFailOnError();
        transactionId = UUID.randomUUID().toString();
        workers = new ArrayList<>();
        completedCount = new AtomicInteger(0);
        errorCount = new AtomicInteger(0);
        for (var foo = 0; foo < config.getReindexingThreads(); foo += 1) {
            workers.add(new ReindexWorker(this, this.reindexService, transactionId, this.failOnError));
        }
    }

    /**
     * Start reindexing.
     * @throws InterruptedException on an indexing error in a thread.
     */
    public void start() throws InterruptedException {
        try {
            workers.forEach(ReindexWorker::start);
            for (final var worker : workers) {
                worker.join();
            }
        } catch (final Exception e) {
            LOGGER.error("Error while rebuilding index", e);
            stop();
            throw e;
        }
    }

    /**
     * Stop all threads.
     */
    public void stop() {
        workers.forEach(ReindexWorker::stopThread);
    }

    /**
     * Return a batch of OCFL ids to reindex.
     * @return list of OCFL ids.
     */
    public synchronized List<String> getIds() {
        int counter = 0;
        final List<String> ids = new ArrayList<>((int) batchSize);
        while (ocflIter.hasNext() && counter < batchSize) {
            ids.add(ocflIter.next());
            counter += 1;
        }
        return ids;
    }

    /**
     * Update the master list of reindexing states.
     * @param batchSuccessful how many items were completed successfully in the last batch.
     * @param batchErrors how many items had an error in the last batch.
     */
    public void updateComplete(final int batchSuccessful, final int batchErrors) {
        completedCount.addAndGet(batchSuccessful);
        errorCount.addAndGet(batchErrors);
    }

    /**
     * @return the count of items that completed successfully.
     */
    public int getCompletedCount() {
        return completedCount.get();
    }

    /**
     * @return the count of items that had errors.
     */
    public int getErrorCount() {
        return errorCount.get();
    }

    /**
     * Commit the actions.
     */
    public void commit() {
        reindexService.commit(transactionId);
        reindexService.indexMembership(transactionId);
    }

    /**
     * Rollback the current transaction.
     */
    public void rollback() {
        reindexService.rollback(transactionId);
    }

    /**
     * Close stream.
     */
    public void shutdown() {
        ocflStream.close();
    }
}
