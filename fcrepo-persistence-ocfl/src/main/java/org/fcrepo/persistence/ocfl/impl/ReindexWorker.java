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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;

import org.slf4j.Logger;

import com.google.common.base.Stopwatch;

/**
 * A reindexing worker thread.
 * @author whikloj
 */
public class ReindexWorker implements Runnable {

    private static final Logger LOGGER = getLogger(ReindexWorker.class);

    private static final long REPORTING_INTERVAL_SECS = 30;

    private Thread t;
    private ReindexManager manager;
    private ReindexService service;
    private boolean running = true;
    private boolean failOnError;
    private TransactionManager txManager;

    /**
     * Basic Constructor
     * @param reindexManager the manager service.
     * @param reindexService the reindexing service.
     * @param transactionManager a transaction manager to generate
     * @param failOnError whether the thread should fail on an error or log and continue.
     */
    public ReindexWorker(final ReindexManager reindexManager, final ReindexService reindexService,
                         final TransactionManager transactionManager, final boolean failOnError) {
        manager = reindexManager;
        service = reindexService;
        txManager = transactionManager;
        this.failOnError = failOnError;
        t = new Thread(this, "ReindexWorker");
    }

    /**
     * Join the thread.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public void join() throws InterruptedException {
        t.join();
    }

    /**
     * Start the thread with this Runnable
     */
    public void start() {
        t.start();
    }

    @Override
    public void run() {
        final var stopwatch = Stopwatch.createStarted();
        while (running) {
            final List<String> ids = manager.getIds();
            if (ids.isEmpty()) {
                stopThread();
                break;
            }

            int completed = 0;
            int errors = 0;

            for (final var id : ids) {
                final Transaction tx = txManager.create();
                if (!running) {
                    break;
                }
                if (stopwatch.elapsed(TimeUnit.SECONDS) > REPORTING_INTERVAL_SECS) {
                    manager.updateComplete(completed, errors);
                    completed = 0;
                    errors = 0;
                    stopwatch.reset().start();
                }
                try {
                    service.indexOcflObject(tx, id);
                    service.commit(tx);
                    completed += 1;
                } catch (final Exception e) {
                    service.rollback(tx);
                    errors += 1;
                    if (failOnError) {
                        stopThread();
                        manager.updateComplete(completed, errors);
                        manager.stop();
                        service.cleanupSession(tx.getId());
                        throw e;
                    }
                    LOGGER.error("Reindexing of OCFL id {} failed", id, e);
                }
                service.cleanupSession(tx.getId());
            }
            manager.updateComplete(completed, errors);
        }
    }

    /**
     * Stop this thread from running once it has completed its current batch.
     */
    public void stopThread() {
        this.running = false;
    }

}
