/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.IOException;
import java.time.Duration;

import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.TransactionClosedException;
import org.fcrepo.kernel.api.exception.TransactionNotFoundException;
import org.fcrepo.kernel.api.lock.ResourceLockManager;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;

import org.fcrepo.search.api.SearchIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * <p>TransactionTest class.</p>
 *
 * @author mohideen
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransactionManagerImplTest {

    private TransactionImpl testTx;

    private TransactionManagerImpl testTxManager;

    @Mock
    private PersistentStorageSessionManager pssManager;

    @Mock
    private PersistentStorageSession psSession;

    @Mock
    private ContainmentIndex containmentIndex;

    @Mock
    private EventAccumulator eventAccumulator;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private SearchIndex searchIndex;

    @Mock
    private ResourceLockManager resourceLockManager;

    @Mock
    private UserTypesCache userTypesCache;

    private FedoraPropsConfig fedoraPropsConfig;

    @BeforeEach
    public void setUp() {
        fedoraPropsConfig = new FedoraPropsConfig();
        fedoraPropsConfig.setSessionTimeout(Duration.ofMillis(180000));
        testTxManager = new TransactionManagerImpl();
        when(pssManager.getSession(any())).thenReturn(psSession);
        setField(testTxManager, "pSessionManager", pssManager);
        setField(testTxManager, "containmentIndex", containmentIndex);
        setField(testTxManager, "searchIndex", searchIndex);
        setField(testTxManager, "eventAccumulator", eventAccumulator);
        setField(testTxManager, "referenceService", referenceService);
        setField(testTxManager, "membershipService", membershipService);
        setField(testTxManager, "dbTransactionExecutor", new DbTransactionExecutor());
        setField(testTxManager, "fedoraPropsConfig", fedoraPropsConfig);
        setField(testTxManager, "resourceLockManager", resourceLockManager);
        setField(testTxManager, "userTypesCache", userTypesCache);
        testTx = (TransactionImpl) testTxManager.create();
    }

    @Test
    public void testCreateTransaction() {
        testTx = (TransactionImpl) testTxManager.create();
        assertNotNull(testTx);
    }

    @Test
    public void testGetTransaction() {
        final TransactionImpl tx = (TransactionImpl) testTxManager.get(testTx.getId());
        assertNotNull(tx);
        assertEquals(testTx.getId(), tx.getId());
    }

    @Test
    public void testGetTransactionWithInvalidID() {
        assertThrows(TransactionNotFoundException.class, () -> testTxManager.get("invalid-id"));
    }

    @Test
    public void testGetExpiredTransaction() throws Exception {
        testTx.expire();
        assertThrows(TransactionClosedException.class, () -> testTxManager.get(testTx.getId()));
        // Make sure rollback is triggered
        verify(psSession).rollback();
    }

    @Test
    public void testCleanupClosedTransactions() {
        fedoraPropsConfig.setSessionTimeout(Duration.ofMillis(10000));

        final var commitTx = testTxManager.create();
        commitTx.commit();
        final var continuingTx = testTxManager.create();
        final var rollbackTx = testTxManager.create();
        rollbackTx.rollback();

        // verify that transactions retrievable before cleanup
        try {
            testTxManager.get(commitTx.getId());
            fail("Transaction must be committed");
        } catch (final TransactionClosedException e) {
            //expected
        }
        try {
            testTxManager.get(rollbackTx.getId());
            fail("Transaction must be rolled back");
        } catch (final TransactionClosedException e) {
            //expected
        }

        assertNotNull(testTxManager.get(continuingTx.getId()),
                "Continuing transaction must be present");

        testTxManager.cleanupClosedTransactions();

        // Verify that the closed transactions are stick around since they haven't expired yet
        assertThrows(TransactionClosedException.class, () -> testTxManager.get(commitTx.getId()));

        assertThrows(TransactionClosedException.class, () -> testTxManager.get(rollbackTx.getId()));

        // Force expiration of the closed transactions, rather than waiting for it
        commitTx.expire();
        rollbackTx.expire();
        testTxManager.cleanupClosedTransactions();

        // verify that closed transactions cleanedup

        verify(pssManager).removeSession(commitTx.getId());
        verify(pssManager).removeSession(rollbackTx.getId());
        verify(pssManager, never()).removeSession(continuingTx.getId());

        assertThrows(TransactionNotFoundException.class, () -> testTxManager.get(commitTx.getId()));
        assertThrows(TransactionNotFoundException.class, () -> testTxManager.get(rollbackTx.getId()));

        assertNotNull(testTxManager.get(continuingTx.getId()),
                "Continuing transaction must be present");
    }

    // Check that the scheduled cleanup process rolls back expired transactions, but leaves
    // them around until the next cleanup call so that they can be queried.
    @Test
    public void testCleanupExpiringTransaction() throws Exception {
        fedoraPropsConfig.setSessionTimeout(Duration.ofMillis(0));

        final var expiringTx = testTxManager.create();

        Thread.sleep(100);

        testTxManager.cleanupClosedTransactions();


        assertThrows(TransactionClosedException.class, () -> testTxManager.get(expiringTx.getId()));

        verify(psSession).rollback();
        verify(pssManager).removeSession(expiringTx.getId());

        testTxManager.cleanupClosedTransactions();

        assertThrows(TransactionNotFoundException.class, () -> testTxManager.get(expiringTx.getId()));
    }

    @Test
    public void testCleanupAllTransactions() {
        final var commitTx = testTxManager.create();
        commitTx.commit();
        final var continuingTx = testTxManager.create();
        final var rollbackTx = testTxManager.create();
        rollbackTx.rollback();
        final var expiredTx = testTxManager.create();
        expiredTx.expire();
        final var expiredAndRolledBackTx = testTxManager.create();
        expiredAndRolledBackTx.expire();
        expiredAndRolledBackTx.rollback();

        testTxManager.cleanupAllTransactions();

        // All transactions must now be gone
        assertThrows(TransactionClosedException.class, () -> testTxManager.get(commitTx.getId()));
        assertThrows(TransactionClosedException.class, () -> testTxManager.get(rollbackTx.getId()));
        assertThrows(TransactionClosedException.class, () -> testTxManager.get(continuingTx.getId()));
        assertThrows(TransactionClosedException.class, () -> testTxManager.get(expiredTx.getId()));
        // Committed transaction does not get rolled back
        verify(pssManager, never()).removeSession(commitTx.getId());
        // Rolled back transactions does not get rolled back (again)
        verify(pssManager, never()).removeSession(rollbackTx.getId());
        // Open and expired transactions get rolled back
        verify(pssManager).removeSession(continuingTx.getId());
        verify(pssManager).removeSession(expiredTx.getId());
        // Unless the expired transaction is already rolled back
        verify(pssManager, never()).removeSession(expiredAndRolledBackTx.getId());
    }

    @Test
    public void testPreCleanTransactions() throws IOException {
        testTxManager.preCleanTransactions();
        verify(containmentIndex).clearAllTransactions();
        verify(membershipService).clearAllTransactions();
        verify(referenceService).clearAllTransactions();
        verify(searchIndex).clearAllTransactions();
        verify(pssManager).clearAllSessions();
    }
}
