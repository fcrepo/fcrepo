/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl;

import com.google.common.base.Stopwatch;
import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    public void setUp() {
        testTx = new TransactionImpl("123", txManager, Duration.ofMillis(180000));
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
        try {
            assertThrows(TransactionClosedException.class, () -> testTx.commit());
        } finally {
            verify(psSession, never()).commit();
        }
    }

    @Test
    public void testCommitRolledbackTx() throws Exception {
        testTx.rollback();
        try {
            assertThrows(TransactionClosedException.class, () -> testTx.commit());
        } finally {
            verify(psSession, never()).commit();
        }
    }

    @Test
    public void testEnsureRollbackOnFailedCommit() throws Exception {
        doThrow(new PersistentStorageException("Failed")).when(psSession).commit();
        try {
            assertThrows(RepositoryRuntimeException.class, () ->testTx.commit());
        } finally {
            verify(psSession).commit();
            verify(psSession).rollback();
        }
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
        doThrow(new RuntimeException()).when(containmentIndex).rollbackTransaction(testTx);
        testTx.rollback();
        verifyRollback();
    }

    @Test
    public void shouldRollbackAllWhenEventsThrowsException() throws Exception {
        doThrow(new RuntimeException()).when(eventAccumulator).clearEvents(testTx);
        testTx.rollback();
        verifyRollback();
    }

    @Test
    public void testRollbackCommited() throws Exception {
        testTx.commit();
        try {
            assertThrows(TransactionClosedException.class, () -> testTx.rollback());
        } finally {
            verify(psSession, never()).rollback();
        }
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
    public void testUpdateExpiry() {
        final Instant previousExpiry = testTx.getExpires();
        testTx.updateExpiry(Duration.ofSeconds(1));
        assertTrue(testTx.getExpires().isAfter(previousExpiry));
    }

    @Test
    public void testUpdateExpiryOnExpired() {
        testTx.expire();
        final Instant previousExpiry = testTx.getExpires();
        try {
            assertThrows(TransactionClosedException.class, () ->testTx.updateExpiry(Duration.ofSeconds(1)));
        } finally {
            assertEquals(testTx.getExpires(), previousExpiry);
        }
    }

    @Test
    public void testRefresh() {
        final Instant previousExpiry = testTx.getExpires();
        testTx.refresh();
        assertTrue(testTx.getExpires().isAfter(previousExpiry));
    }

    @Test
    public void testRefreshOnExpired() {
        testTx.expire();
        final Instant previousExpiry = testTx.getExpires();
        try {
            assertThrows(TransactionClosedException.class, () ->testTx.refresh());
        } finally {
            assertEquals(testTx.getExpires(), previousExpiry);
        }
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
}
