/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.common.db;

import static org.slf4j.LoggerFactory.getLogger;

import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;

/**
 * Wrapper around Spring's db transaction management
 *
 * @author pwinckles
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DbTransactionExecutor {

    private static final Logger LOGGER = getLogger(DbTransactionExecutor.class);

    private static final RetryPolicy<Object> DB_RETRY = RetryPolicy.builder()
            .handleIf(DbTransactionExecutor::isLockFailure)
            .onRetry(event -> {
                LOGGER.debug("Retrying operation that failed with the following exception", event.getLastException());
            })
            .withBackoff(10, 100, ChronoUnit.MILLIS, 1.5)
            .withJitter(0.1)
            .withMaxRetries(10)
            .build();

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Spring 6 translates deadlocks to PessimisticLockingFailureException or its subclass
     * CannotAcquireLockException, depending on the database, and callers often wrap it.
     *
     * @param e the exception to check
     * @return true if e or any of its causes is a pessimistic locking failure
     */
    static boolean isLockFailure(final Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof PessimisticLockingFailureException) {
                return true;
            }
        }
        return false;
    }

    public DbTransactionExecutor() {

    }

    public DbTransactionExecutor(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Executes the runnable within a DB transaction that will retry entire block on deadlocks and lock failures.
     *
     * @param action the code to execute
     */
    public void doInTxWithRetry(final Runnable action) {
        Failsafe.with(DB_RETRY).run(() -> {
            doInTx(action);
        });
    }

    /**
     * Executes the runnable within a DB transaction. Deadlocks and lock failures are NOT retried.
     *
     * @param action the code to execute
     */
    public void doInTx(final Runnable action) {
        if (transactionTemplate == null) {
            // If the transaction template is not set, just execute the code without a tx.
            // This will never happen when configured by Spring, but is useful when unit testing
            LOGGER.warn("Executing outside of a DB transaction");
            action.run();
        } else {
            transactionTemplate.executeWithoutResult(status -> {
                action.run();
            });
        }
    }

}
