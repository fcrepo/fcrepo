/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.common.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * @author Dan Field
 */
public class DbTransactionExecutorTest {

    private final DbTransactionExecutor executor = new DbTransactionExecutor();

    @Test
    public void testIsLockFailure() {
        // PostgreSQL deadlock (SQLSTATE 40P01) under Spring 6
        assertTrue(DbTransactionExecutor.isLockFailure(new PessimisticLockingFailureException("deadlock")));
        // MySQL deadlock / PostgreSQL serialization failure (SQLSTATE 40001) under Spring 6
        assertTrue(DbTransactionExecutor.isLockFailure(new CannotAcquireLockException("deadlock")));
        // Wrapped more than once
        assertTrue(DbTransactionExecutor.isLockFailure(new RuntimeException("outer",
                new RuntimeException("inner", new PessimisticLockingFailureException("deadlock")))));

        assertFalse(DbTransactionExecutor.isLockFailure(new DataIntegrityViolationException("constraint")));
        assertFalse(DbTransactionExecutor.isLockFailure(new RuntimeException("outer",
                new IllegalStateException("inner"))));
    }

    @Test
    public void testRetriesWrappedDeadlock() {
        final var attempts = new AtomicInteger();
        executor.doInTxWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("Unable to commit containment index transaction",
                        new PessimisticLockingFailureException("deadlock detected"));
            }
        });
        assertEquals(3, attempts.get());
    }

    @Test
    public void testDoesNotRetryOtherFailures() {
        final var attempts = new AtomicInteger();
        final var failure = new DataIntegrityViolationException("constraint");
        final var thrown = assertThrows(DataIntegrityViolationException.class, () ->
                executor.doInTxWithRetry(() -> {
                    attempts.incrementAndGet();
                    throw failure;
                }));
        assertSame(failure, thrown);
        assertEquals(1, attempts.get());
    }

    @Test
    public void testGivesUpAfterMaxRetries() {
        final var attempts = new AtomicInteger();
        assertThrows(CannotAcquireLockException.class, () ->
                executor.doInTxWithRetry(() -> {
                    attempts.incrementAndGet();
                    throw new CannotAcquireLockException("deadlock");
                }));
        assertEquals(11, attempts.get());
    }

    @Test
    public void testDoInTxDoesNotRetry() {
        final var attempts = new AtomicInteger();
        assertThrows(PessimisticLockingFailureException.class, () ->
                executor.doInTx(() -> {
                    attempts.incrementAndGet();
                    throw new PessimisticLockingFailureException("deadlock");
                }));
        assertEquals(1, attempts.get());
    }
}
