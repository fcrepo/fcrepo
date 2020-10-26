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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * Class to coordinate the index rebuilding tasks.
 * @author whikloj
 */
public class ReindexManager {

    private static final Logger LOGGER = getLogger(ReindexManager.class);

    private final String transactionId;

    private final List<ReindexWorker> workers;

    private final Iterator<String> ocflIter;

    private final Map<String, String> resultStates = new HashMap<>();

    private final ReindexService reindexService;

    private final int batchSize;

    /**
     * Basic constructor
     * @param ids stream of ocfl ids.
     * @param reindexService the reindexing service.
     * @param failOnError whether to have threads fail on an error or log and continue.
     * @param batchSize number of ids to distribute per request.
     */
    public ReindexManager(final Stream<String> ids, final ReindexService reindexService, final boolean failOnError,
                          final int batchSize) {
        this.ocflIter = ids.iterator();
        this.reindexService = reindexService;
        this.batchSize = batchSize;
        transactionId = UUID.randomUUID().toString();
        workers = new ArrayList<>();
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        final int threads = availableProcessors > 1 ? availableProcessors - 1 : 1;
        for (var foo = 0; foo < threads; foo += 1) {
            workers.add(new ReindexWorker(this, this.reindexService, failOnError));
        }
    }

    /**
     * Get the transaction id for the reindexing run.
     * @return the transaction id.
     */
    public String getTransactionId() {
        return transactionId;
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
        for (final var worker : workers) {
            worker.stopThread();
        }
    }

    /**
     * Return a batch of OCFL ids to reindex.
     * @return list of OCFL ids.
     */
    public synchronized List<String> getIds() {
        int counter = 0;
        final List<String> ids = new ArrayList<>();
        while (ocflIter.hasNext() && counter < batchSize) {
            ids.add(ocflIter.next());
            counter += 1;
        }
        return ids;
    }

    /**
     * Update the master list of reindexing states.
     * @param newStates map of ocflIds to empty string on success or some error message.
     */
    public void updateComplete(final Map<String, String> newStates) {
        resultStates.putAll(newStates);
    }

    /**
     * Get the result states map.
     * @return map of ocflIds to empty string on success or some error message.
     */
    public Map<String, String> getResultStates() {
        return resultStates;
    }

    /**
     * Commit the actions.
     */
    public void commit() {
        reindexService.commit(transactionId);
    }

}
