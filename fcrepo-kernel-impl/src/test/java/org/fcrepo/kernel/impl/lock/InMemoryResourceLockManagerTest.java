/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import javax.inject.Inject;

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLockManager;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author pwinckles
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class InMemoryResourceLockManagerTest {

    private ResourceLockManager lockManager;

    private static ExecutorService executor;

    @Inject
    private ContainmentIndex containmentIndex;

    @Mock
    private Transaction tx1;

    @Mock
    private Transaction tx2;

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
    @FlywayTest
    public void setup() {
        MockitoAnnotations.openMocks(this);

        lockManager = new InMemoryResourceLockManager();

        txId1 = UUID.randomUUID().toString();
        when(tx1.getId()).thenReturn(txId1);
        when(tx1.isOpenLongRunning()).thenReturn(false);
        txId2 = UUID.randomUUID().toString();
        when(tx2.getId()).thenReturn(txId2);
        when(tx2.isOpenLongRunning()).thenReturn(false);
        resourceId = randomResourceId();

        setField(lockManager, "containmentIndex", containmentIndex);
    }

    @Test
    public void shouldLockResourceWhenNotAlreadyLocked() {
        lockManager.acquire(tx1, resourceId);
    }

    @Test
    public void sameTxShouldBeAbleToReacquireLockItAlreadyHolds() {
        lockManager.acquire(tx1, resourceId);
        lockManager.acquire(tx1, resourceId);
    }

    @Test
    public void shouldFailToAcquireLockWhenHeldByAnotherTx() {
        lockManager.acquire(tx1, resourceId);
        assertLockException(() -> {
            lockManager.acquire(tx2, resourceId);
        });
    }

    @Test
    public void shouldAcquireLockAfterReleasedByAnotherTx() {
        lockManager.acquire(tx1, resourceId);
        lockManager.releaseAll(txId1);
        lockManager.acquire(tx2, resourceId);
    }

    @Test
    public void concurrentRequestsFromSameTxShouldBothSucceedWhenLockAvailable()
            throws ExecutionException, InterruptedException {
        final var phaser = new Phaser(3);

        final var future1 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            lockManager.acquire(tx1, resourceId);
            return true;
        });
        final var future2 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            lockManager.acquire(tx1, resourceId);
            return true;
        });

        phaser.arriveAndAwaitAdvance();

        assertTrue(future1.get());
        assertTrue(future2.get());
    }

    @Test
    public void concurrentRequestsFromDifferentTxesOnlyOneShouldSucceed()
            throws ExecutionException, InterruptedException {
        final var phaser = new Phaser(3);

        final var future1 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(tx1, resourceId);
                return true;
            } catch (final ConcurrentUpdateException e) {
                return false;
            }
        });
        final var future2 = executor.submit(() -> {
            phaser.arriveAndAwaitAdvance();
            try {
                lockManager.acquire(tx2, resourceId);
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
    public void releasingAlreadyReleasedLocksShouldDoNothing() {
        lockManager.acquire(tx1, resourceId);
        lockManager.releaseAll(txId1);
        lockManager.releaseAll(txId1);
        lockManager.acquire(tx2, resourceId);
    }

    @Test
    public void cannotCreateGhostNode() {
        final var resourceId2 = resourceId.resolve(UUID.randomUUID().toString());
        lockManager.acquire(tx1, resourceId2);
        assertLockException(() -> {
            lockManager.acquire(tx2, resourceId);
        });
    }

    private void assertLockException(final Runnable runnable) {
        try {
            runnable.run();
            fail("acquire should have thrown an exception");
        } catch (final ConcurrentUpdateException e) {
            // expected exception
        }
    }

    private FedoraId randomResourceId() {
        return FedoraId.create(UUID.randomUUID().toString());
    }

}
