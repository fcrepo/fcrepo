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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A reindexing worker thread.
 * @author whikloj
 */
public class ReindexWorker implements Runnable {

    private Thread t;
    private ReindexManager manager;
    private ReindexService service;
    private boolean running = true;
    private Map<String, String> states = new HashMap<>();
    private boolean failOnError;

    /**
     * Basic Constuctor
     * @param reindexManager the manager service.
     * @param reindexService the reindexing service.
     * @param failOnError whether the thread should fail on an error or log and continue.
     */
    public ReindexWorker(final ReindexManager reindexManager, final ReindexService reindexService,
                         final boolean failOnError) {
        manager = reindexManager;
        service = reindexService;
        this.failOnError = failOnError;
    }

    public void join() throws InterruptedException {
        t.join();
    }

    public void start() {
        t = new Thread(this, "ReindexWorker");
        t.start();
    }

    public void run() {
        while (running) {
            final List<String> ids = manager.getIds();
            if (ids.isEmpty()) {
                stopThread();
                break;
            }
            for (final var id : ids) {
                try {
                    service.indexOcflObject(manager.getTransactionId(), id);
                    states.put(id, "");
                } catch (final Exception e) {
                    states.put(id, e.getMessage());
                    if (failOnError) {
                        stopThread();
                        manager.updateComplete(states);
                        throw e;
                    }
                }
            }
            service.cleanupSession(manager.getTransactionId());
        }
        manager.updateComplete(states);
    }

    /**
     * Stop this thread from running once it has completed its current batch.
     */
    public void stopThread() {
        this.running = false;
    }
}
