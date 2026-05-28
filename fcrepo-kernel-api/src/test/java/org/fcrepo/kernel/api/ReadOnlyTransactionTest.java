/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ReadOnlyTransaction}
 *
 * @author whikloj
 */
public class ReadOnlyTransactionTest {

    private ReadOnlyTransaction transaction;

    @BeforeEach
    public void setUp() {
        transaction = ReadOnlyTransaction.INSTANCE;
    }

    @Test
    public void testExpiration() {
        assertFalse(transaction.hasExpired());
        transaction.expire(); // ReadOnlyTransaction cannot be expired
        assertFalse(transaction.hasExpired());
        // ReadOnlyTransaction has an expiration time of one minute after now
        assertTrue(transaction.getExpires().isAfter(Instant.now()) &&
                transaction.getExpires().minus(Duration.ofMinutes(2)).isBefore(Instant.now()));
        transaction.updateExpiry(Duration.ofHours(1));
        // ReadOnlyTransaction ALWAYS has an expiration time of one minute after now
        assertTrue(transaction.getExpires().isAfter(Instant.now()) &&
                transaction.getExpires().minus(Duration.ofMinutes(2)).isBefore(Instant.now()));
    }

    @Test
    public void testIs() {
        assertTrue(transaction.isOpen());
        assertFalse(transaction.isOpenLongRunning());
        assertTrue(transaction.isReadOnly());
        assertFalse(transaction.isRolledBack());
        assertFalse(transaction.isCommitted());
        assertTrue(transaction.isShortLived());
        transaction.setShortLived(false); // Can't change ReadOnlyTransaction
        assertTrue(transaction.isShortLived());
    }

    @Test
    public void testDoInTx() {
        assertThrows(IllegalArgumentException.class, () -> {
            transaction.doInTx(() -> {
                throw new IllegalArgumentException("Expected exception");
            });
        });
    }

    @Test
    public void testGetId() {
        assertEquals(ReadOnlyTransaction.READ_ONLY_TX_ID, transaction.getId());
    }
}
