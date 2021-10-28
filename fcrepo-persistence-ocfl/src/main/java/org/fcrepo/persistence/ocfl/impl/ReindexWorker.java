/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fcrepo.common.db.DbTransactionExecutor;
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
    private DbTransactionExecutor dbTransactionExecutor;

    /**
     * Basic Constructor
     * @param name the name of the worker -- used in logging
     * @param reindexManager the manager service.
     * @param reindexService the reindexing service.
     * @param transactionManager a transaction manager to generate
     * @param dbTransactionExecutor manages db transactions
     * @param failOnError whether the thread should fail on an error or log and continue.
     */
    public ReindexWorker(final String name,
                         final ReindexManager reindexManager,
                         final ReindexService reindexService,
                         final TransactionManager transactionManager,
                         final DbTransactionExecutor dbTransactionExecutor,
                         final boolean failOnError) {
        manager = reindexManager;
        service = reindexService;
        txManager = transactionManager;
        this.dbTransactionExecutor = dbTransactionExecutor;
        this.failOnError = failOnError;
        t = new Thread(this, name);
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
                LOGGER.debug("No more objects found to process. Stopping...");
                stopThread();
                break;
            }

            int completed = 0;
            int errors = 0;

            for (final var id : ids) {
                if (!running) {
                    break;
                }

                final Transaction tx = txManager.create();
                tx.setShortLived(true);
                if (stopwatch.elapsed(TimeUnit.SECONDS) > REPORTING_INTERVAL_SECS) {
                    manager.updateComplete(completed, errors);
                    completed = 0;
                    errors = 0;
                    stopwatch.reset().start();
                }
                try {
                    dbTransactionExecutor.doInTxWithRetry(() -> {
                        service.indexOcflObject(tx, id);
                        tx.commit();
                    });
                    completed += 1;
                } catch (final Exception e) {
                    LOGGER.error("Reindexing of OCFL id {} failed", id, e);
                    tx.rollback();
                    errors += 1;
                    if (failOnError) {
                        manager.updateComplete(completed, errors);
                        manager.stop();
                        service.cleanupSession(tx.getId());
                        throw e;
                    }
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
