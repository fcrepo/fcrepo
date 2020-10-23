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

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

/**
 * Class to coordinate the index rebuilding tasks.
 * @author whikloj
 */
public class ReindexManager {

    private static final Logger LOGGER = getLogger(ReindexManager.class);

    private final ExecutorService executorService;

    private final ReindexService reindexService;

    private final AtomicLong count;

    private final Object lock;

    private final AtomicLong complete;

    private final String transactionId;

    /**
     * Basic constructor
     * @param execSrvc the executorservice
     * @param reindexSrvc the ReindexService.
     */
    public ReindexManager(final ExecutorService execSrvc, final ReindexService reindexSrvc) {
        executorService = execSrvc;
        reindexService = reindexSrvc;
        count = new AtomicLong(0);
        complete = new AtomicLong(0);
        lock = new Object();
        transactionId = UUID.randomUUID().toString();
    }

    /**
     * Add a new task to the executor service.
     * @param id the OCFL id to reindex.
     */
    public void submit(final String id) {
        final var task = new ReindexTask(transactionId, id, reindexService);

        executorService.submit(() -> {
            try {
                task.run();
            } finally {
                count.decrementAndGet();
                complete.incrementAndGet();
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        });

        count.incrementAndGet();
    }

    /**
     * Blocks until all migration tasks are complete. Note, this does not prevent additional tasks from being submitted.
     * It simply waits until the queue is empty.
     *
     * @throws InterruptedException on interrupt
     */
    public void awaitCompletion() throws InterruptedException {
        LOGGER.info("OCFL objects to index: {}", count.get());
        if (count.get() == 0) {
            return;
        }

        synchronized (lock) {
            while (count.get() > 0) {
                lock.wait();
            }
        }
    }

    /**
     * Commit the actions, shut down the executor and closes all resources.
     *
     * @throws InterruptedException on interrupt
     */
    public void shutdown() throws InterruptedException {
        reindexService.commit(transactionId);
        executorService.shutdown();
        if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
            LOGGER.error("Failed to shutdown executor service cleanly after 1 minute of waiting");
            executorService.shutdownNow();
        }
    }

    /**
     * Get the number of completed ocfl objects rebuilt.
     * @return the completed count.
     */
    public long getProcessed() {
        return complete.get();
    }
}
