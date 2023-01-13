/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.lock.ResourceLockType;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author pwinckles
 */
public class InMemoryResourceLockManagerTest {

    private ResourceLockManager lockManager;

    private static ExecutorService executor;

    private String txId1;
    private String txId2;
    private FedoraId resourceId;

    @BeforeClass
    public static void beforeClass() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterClass
    public static void afterClass() {
        executor.shutdown();
    }

    @Before
    public void setup() {
        lockManager = new InMemoryResourceLockManager();
        txId1 = UUID.randomUUID().toString();
        txId2 = UUID.randomUUID().toString();
        resourceId = randomResourceId();
    }

    @Test
    public void shouldLockResourceWhenNotAlreadyLockedExclusive() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void shouldLockResourceWhenNotAlreadyLockedNonExclusive() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void sameTxShouldBeAbleToReacquireLockItAlreadyHoldsExclusive() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
        lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void sameTxShouldBeAbleToReacquireLockItAlreadyHoldsNonExclusive() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
        lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void shouldFailToAcquireLockWhenHeldByAnotherTxExclusive() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
        assertLockException(() -> {
            lockManager.acquire(txId2, resourceId, ResourceLockType.EXCLUSIVE);
        });
        assertLockException(() -> {
            lockManager.acquire(txId2, resourceId, ResourceLockType.NONEXCLUSIVE);
        });
    }

    @Test
    public void shouldFailToAcquireLockWhenHeldByAnotherTxSecondExclusive() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
        assertLockException(() -> {
            lockManager.acquire(txId2, resourceId, ResourceLockType.EXCLUSIVE);
        });
    }

    @Test
    public void shouldSucceedToAcquireNonExclusiveLockWhenHeldByAnotherTxNonExclusive() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
        lockManager.acquire(txId2, resourceId, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void shouldAcquireLockAfterReleasedByAnotherTx1() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
        lockManager.releaseAll(txId1);
        lockManager.acquire(txId2, resourceId, ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void shouldAcquireLockAfterReleasedByAnotherTx2() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
        lockManager.releaseAll(txId1);
        lockManager.acquire(txId2, resourceId, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void shouldAcquireLockAfterReleasedByAnotherTx3() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
        lockManager.releaseAll(txId1);
        lockManager.acquire(txId2, resourceId, ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void shouldAcquireLockAfterReleasedByAnotherTx4() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
        lockManager.releaseAll(txId1);
        lockManager.acquire(txId2, resourceId, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void concurrentRequestsFromSameTxShouldBothSucceedWhenLockAvailable()
            throws ExecutionException, InterruptedException {
        final var phaser = new Phaser(3);

        final var future1 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
            return true;
        });
        final var future2 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
            return true;
        });

        phaser.arriveAndAwaitAdvance();

        assertTrue(future1.get());
        assertTrue(future2.get());
    }

    @Test
    public void concurrentExclusiveRequestsFromDifferentTxesOnlyOneShouldSucceed()
            throws ExecutionException, InterruptedException {
        final var phaser = new Phaser(3);

        final var future1 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
                return true;
            } catch (final ConcurrentUpdateException e) {
                return false;
            }
        });
        final var future2 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(txId2, resourceId, ResourceLockType.EXCLUSIVE);
                return true;
            } catch (final ConcurrentUpdateException e) {
                return false;
            }
        });

        phaser.arriveAndAwaitAdvance();

        if (future1.get()) {
            assertFalse("Only one tx should have acquired a lock", future2.get());
        } else {
            assertTrue("Only one tx should have acquired a lock", future2.get());
        }
    }

    @Test
    public void concurrentOneExclusiveRequestsFromDifferentTxesOnlyOneShouldSucceed()
            throws ExecutionException, InterruptedException {
        final var phaser = new Phaser(3);

        final var future1 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
                return true;
            } catch (final ConcurrentUpdateException e) {
                return false;
            }
        });
        final var future2 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(txId2, resourceId, ResourceLockType.NONEXCLUSIVE);
                return true;
            } catch (final ConcurrentUpdateException e) {
                return false;
            }
        });

        phaser.arriveAndAwaitAdvance();

        if (future1.get()) {
            assertFalse("Only one tx should have acquired a lock", future2.get());
        } else {
            assertTrue("Only one tx should have acquired a lock", future2.get());
        }
    }

    @Test
    public void concurrentNonexclusiveRequestsFromDifferentTxesBothShouldSucceed()
            throws ExecutionException, InterruptedException {
        final var phaser = new Phaser(3);

        final var future1 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(txId1, resourceId, ResourceLockType.NONEXCLUSIVE);
                return true;
            } catch (final ConcurrentUpdateException e) {
                return false;
            }
        });
        final var future2 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(txId2, resourceId, ResourceLockType.NONEXCLUSIVE);
                return true;
            } catch (final ConcurrentUpdateException e) {
                return false;
            }
        });

        phaser.arriveAndAwaitAdvance();
        // Both should succeed.
        assertTrue(future1.get());
        assertTrue(future2.get());
    }

    @Test
    public void releasingAlreadyReleasedLocksShouldDoNothing() {
        lockManager.acquire(txId1, resourceId, ResourceLockType.EXCLUSIVE);
        lockManager.releaseAll(txId1);
        lockManager.releaseAll(txId1);
        lockManager.acquire(txId2, resourceId, ResourceLockType.EXCLUSIVE);
    }

    private void assertLockException(final Runnable runnable) {
        try {
            runnable.run();
            fail("acquire should have thrown an exception");
        } catch (ConcurrentUpdateException e) {
            // expected exception
        }
    }

    private FedoraId randomResourceId() {
        return FedoraId.create(UUID.randomUUID().toString());
    }

}
