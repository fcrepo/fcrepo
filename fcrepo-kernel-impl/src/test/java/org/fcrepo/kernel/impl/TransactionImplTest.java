/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Stopwatch;
import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionState;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.exception.TransactionRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.search.api.SearchIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * TransactionTest class.
 * </p>
 *
 * @author mohideen
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransactionImplTest {

    private TransactionImpl testTx;

    @Mock
    private TransactionManagerImpl txManager;

    @Mock
    private PersistentStorageSessionManager pssManager;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ContainmentIndex containmentIndex;

    @Mock
    private SearchIndex searchIndex;

    @Mock
    private EventAccumulator eventAccumulator;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private ResourceLockManager resourceLockManager;

    @Mock
    private UserTypesCache userTypesCache;

    private static final long DEFAULT_SESSION_MILLI = 180000;
    private static final Duration DEFAULT_SESSION_DURATION = Duration.ofMillis(DEFAULT_SESSION_MILLI);

    @BeforeEach
    public void setUp() {
        testTx = new TransactionImpl("123", txManager, DEFAULT_SESSION_DURATION);
        when(pssManager.getSession(testTx)).thenReturn(psSession);
        when(txManager.getPersistentStorageSessionManager()).thenReturn(pssManager);
        when(txManager.getContainmentIndex()).thenReturn(containmentIndex);
        when(txManager.getEventAccumulator()).thenReturn(eventAccumulator);
        when(txManager.getReferenceService()).thenReturn(referenceService);
        when(txManager.getMembershipService()).thenReturn(membershipService);
        when(txManager.getSearchIndex()).thenReturn(this.searchIndex);
        when(txManager.getDbTransactionExecutor()).thenReturn(new DbTransactionExecutor());
        when(txManager.getResourceLockManager()).thenReturn(resourceLockManager);
        when(txManager.getUserTypesCache()).thenReturn(userTypesCache);
    }

    @Test
    public void testGetId() {
        assertEquals("123", testTx.getId());
    }

    @Test
    public void testDefaultShortLived() {
        assertEquals(true, testTx.isShortLived());
    }

    @Test
    public void testSetShortLived() {
        testTx.setShortLived(false);
        assertEquals(false, testTx.isShortLived());
    }

    @Test
    public void testCommit() throws Exception {
        testTx.commit();
        verify(psSession).commit();
    }

    @Test
    public void testCommitIfShortLived() throws Exception {
        testTx.setShortLived(true);
        testTx.commitIfShortLived();
        verify(psSession).commit();
    }

    @Test
    public void testCommitIfShortLivedOnNonShortLived() throws Exception {
        testTx.setShortLived(false);
        testTx.commitIfShortLived();
        verify(psSession, never()).commit();
    }

    @Test
    public void testCommitExpired() throws Exception {
        testTx.expire();
        assertThrows(TransactionClosedException.class, () -> testTx.commit());
        verify(psSession, never()).commit();
    }

    @Test
    public void testCommitRolledbackTx() throws Exception {
        testTx.rollback();
        assertThrows(TransactionClosedException.class, () -> testTx.commit());
        verify(psSession, never()).commit();
    }

    @Test
    public void testEnsureRollbackOnFailedCommit() throws Exception {
        doThrow(new PersistentStorageException("Failed")).when(psSession).commit();
        assertThrows(RepositoryRuntimeException.class, () -> testTx.commit());
        verify(psSession).commit();
        verify(psSession).rollback();
    }

    @Test
    public void testCommitAlreadyCommittedTx() throws Exception {
        testTx.commit();
        testTx.commit();
        verify(psSession, times(1)).commit();
    }

    @Test
    public void testRollback() throws Exception {
        testTx.rollback();
        verify(psSession).rollback();
    }

    @Test
    public void shouldRollbackAllWhenStorageThrowsException() throws Exception {
        doThrow(new PersistentStorageException("storage")).when(psSession).rollback();
        testTx.rollback();
        verifyRollback();
    }

    @Test
    public void shouldRollbackAllWhenContainmentThrowsException() throws Exception {
        doThrow(new RuntimeException("Rollback exception")).when(containmentIndex).rollbackTransaction(testTx);
        testTx.rollback();
        verifyRollback();
    }

    @Test
    public void shouldRollbackAllWhenEventsThrowsException() throws Exception {
        doThrow(new RuntimeException("Rollback exception")).when(eventAccumulator).clearEvents(testTx);
        testTx.rollback();
        verifyRollback();
    }

    @Test
    public void testRollbackCommited() throws Exception {
        testTx.commit();
        assertThrows(TransactionClosedException.class, () -> testTx.rollback());
        verify(psSession, never()).rollback();
    }

    @Test
    public void testRollbackAlreadyRolledbackTx() throws Exception {
        testTx.rollback();
        testTx.rollback();
        verify(psSession, times(1)).rollback();
    }

    @Test
    public void testExpire() {
        testTx.expire();
        assertTrue(testTx.hasExpired());
    }

    @Test
    public void testUpdateExpiry() throws Exception {
        final Instant previousExpiry = testTx.getExpires();
        assertEquals(previousExpiry, testTx.getExpires());
        // Initial expiry should be within 1 second of current time + default session duration
        assertExpiresIsInRange(testTx, 1);

        Thread.sleep(100);
        // First update to expiration
        testTx.updateExpiry(DEFAULT_SESSION_DURATION);
        final var updatedExpiry = testTx.getExpires();
        // Expiration should be roughly default session duration from now still
        assertExpiresIsInRange(testTx, 1);
        // But the expiry should not match the original expiry
        assertNotEquals(previousExpiry, updatedExpiry);

        Thread.sleep(100);
        // Update again after a second, expiration should still be roughly default session duration from now
        testTx.updateExpiry(DEFAULT_SESSION_DURATION);
        assertExpiresIsInRange(testTx, 1);
        // But the expiry should not match the previous updated expiry
        assertNotEquals(updatedExpiry, testTx.getExpires());

    }

    private void assertExpiresIsInRange(final Transaction testTx, final int plusMinusSeconds) {
        final var currentInstant = Instant.now();
        final var expected = currentInstant.plus(DEFAULT_SESSION_DURATION);
        final var lowerBound = expected.minusSeconds(plusMinusSeconds);
        final var upperBound = expected.plusSeconds(plusMinusSeconds);
        final var expires = testTx.getExpires();
        assertTrue(expires.isAfter(lowerBound) && expires.isBefore(upperBound),
                "Expires does not match expected value +- " + plusMinusSeconds + " secs."
                        + " expected expires: " + expected + ", actual expires: "  + expires);
    }

    @Test
    public void testUpdateExpiryOnExpired() {
        testTx.expire();
        final Instant previousExpiry = testTx.getExpires();
        assertThrows(TransactionClosedException.class, () -> testTx.updateExpiry(Duration.ofSeconds(1)));
        assertEquals(testTx.getExpires(), previousExpiry);
    }

    @Test
    public void testRefresh() throws Exception {
        final Instant previousExpiry = testTx.getExpires();
        Thread.sleep(1000);
        testTx.refresh();
        assertTrue(testTx.getExpires().isAfter(previousExpiry));
    }

    @Test
    public void testRefreshOnExpired() {
        testTx.expire();
        final Instant previousExpiry = testTx.getExpires();
        assertThrows(TransactionClosedException.class, () -> testTx.refresh());
        assertEquals(testTx.getExpires(), previousExpiry);
    }

    @Test
    public void testNewTransactionNotExpired() {
        assertTrue(testTx.getExpires().isAfter(Instant.now()));
    }

    @Test
    public void operationsShouldFailWhenTxNotOpen() {
        testTx.commit();
        assertThrows(TransactionClosedException.class, () -> testTx.doInTx(() -> {
            fail("This code should not be executed");
        }));
    }

    @Test
    public void commitShouldWaitTillAllOperationsComplete() {
        final var executor = Executors.newCachedThreadPool();
        final var phaser = new Phaser(2);

        executor.submit(() -> {
            testTx.doInTx(() -> {
                phaser.arriveAndAwaitAdvance();
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        phaser.arriveAndAwaitAdvance();
        final var stopwatch = Stopwatch.createStarted();
        testTx.commit();
        final var duration = stopwatch.stop().elapsed().toMillis();

        assertTrue(duration < 3000 && duration > 1000);
    }

    private void verifyRollback() throws PersistentStorageException {
        verify(psSession).rollback();
        verify(containmentIndex).rollbackTransaction(testTx);
        verify(eventAccumulator).clearEvents(testTx);
    }

    @Test
    public void testSuppressEvents() {
        testTx.suppressEvents();
        testTx.commit();
        verify(eventAccumulator, times(0))
                .emitEvents(testTx, null, null);
    }

    @Test
    public void testNoEventSuppression() {
        testTx.commit();
        verify(eventAccumulator, times(1))
                .emitEvents(testTx, null, null);
    }



    @Test
    public void testFail() {
        // Transaction should initially be open
        assertTrue(testTx.isOpen());

        // Mark transaction as failed
        testTx.fail();

        // Transaction should no longer be open
        assertFalse(testTx.isOpen());

        // Try to commit should throw exception
        assertThrows(TransactionRuntimeException.class, () -> testTx.commit());
    }

    @Test
    public void testFailWhenNotOpen() {
        // First put transaction in a non-open state
        testTx.commit();

        // Mark as failed should not change state
        testTx.fail();

        // Should still be in committed state
        assertTrue(testTx.isCommitted());
    }

    @Test
    public void testIsRolledBack() {
        // Initially should not be rolled back
        assertFalse(testTx.isRolledBack());

        // Roll it back
        testTx.rollback();

        // Now it should be rolled back
        assertTrue(testTx.isRolledBack());
    }

    @Test
    public void testIsOpenLongRunning() {
        // By default, transaction is short-lived and open
        assertTrue(testTx.isOpen());
        assertFalse(testTx.isOpenLongRunning());

        // Make it long-running
        testTx.setShortLived(false);

        // Now it should be open long-running
        assertTrue(testTx.isOpenLongRunning());

        // Commit the transaction
        testTx.commit();

        // Should no longer be open long-running
        assertFalse(testTx.isOpenLongRunning());
    }

    @Test
    public void testIsOpen() {
        // Initially should be open
        assertTrue(testTx.isOpen());

        // After commit, it shouldn't be open
        testTx.commit();
        assertFalse(testTx.isOpen());
    }

    @Test
    public void testIsOpenRollback() {
        testTx.rollback();
        assertFalse(testTx.isOpen());
    }

    @Test
    public void testIsOpenExpired() {
        testTx.expire();
        assertFalse(testTx.isOpen());
    }

    @Test
    public void testEnsureCommitting() {
        // Initially the transaction is not committing
        assertThrows(TransactionRuntimeException.class, () -> testTx.ensureCommitting());

        testTx.updateState(TransactionState.COMMITTING);
        // It should now pass
        testTx.ensureCommitting();
    }

    @Test
    public void testIsReadOnly() {
        // TransactionImpl is never read-only
        assertFalse(testTx.isReadOnly());
    }

    @Test
    public void testLockResource() {
        final FedoraId resourceId = FedoraId.create("test-resource");

        testTx.lockResource(resourceId);

        verify(resourceLockManager).acquireExclusive(testTx.getId(), resourceId);
    }

    @Test
    public void testLockResourceNonExclusive() {
        final FedoraId resourceId = FedoraId.create("test-resource");

        testTx.lockResourceNonExclusive(resourceId);

        verify(resourceLockManager).acquireNonExclusive(testTx.getId(), resourceId);
    }

    @Test
    public void testLockResourceAndGhostNodes() {
        final FedoraId resourceId = FedoraId.create("test/nested/resource");
        final FedoraId parentId = FedoraId.create("test");

        // Mock the containment index to return the parent id
        when(containmentIndex.getContainerIdByPath(testTx, resourceId, false))
                .thenReturn(parentId);

        testTx.lockResourceAndGhostNodes(resourceId);

        // Should lock the resource itself
        verify(resourceLockManager).acquireExclusive(testTx.getId(), resourceId);

        // Should also lock ghost nodes
        verify(resourceLockManager).acquireExclusive(testTx.getId(), FedoraId.create("test/nested"));
    }

    @Test
    public void testReleaseResourceLocksIfShortLived() {
        // Default transaction is short-lived
        testTx.releaseResourceLocksIfShortLived();

        // Should release all locks
        verify(resourceLockManager).releaseAll(testTx.getId());
    }

    @Test
    public void testReleaseResourceLocksIfNotShortLived() {
        // Make transaction long-running
        testTx.setShortLived(false);

        testTx.releaseResourceLocksIfShortLived();

        // Should not release locks
        verify(resourceLockManager, never()).releaseAll(testTx.getId());
    }

    @Test
    public void testCommitLongRunning() throws Exception {
        // Make transaction long-running
        testTx.setShortLived(false);

        testTx.commit();

        // For long-running transactions, the transaction index should be committed
        verify(containmentIndex).commitTransaction(testTx);
        verify(referenceService).commitTransaction(testTx);
        verify(membershipService).commitTransaction(testTx);
        verify(searchIndex).commitTransaction(testTx);

        // Storage session should be committed
        verify(psSession).prepare();
        verify(psSession).commit();

        // User types cache should be merged
        verify(userTypesCache).mergeSessionCache(testTx.getId());
    }

    @Test
    public void testToString() {
        assertEquals("123", testTx.toString());
    }

    @Test
    public void testConstructorWithNoId() {
        assertThrows(IllegalArgumentException.class,
                () -> new TransactionImpl(null, txManager, DEFAULT_SESSION_DURATION));
    }
}
