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
package org.fcrepo.kernel.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * <p>TransactionTest class.</p>
 *
 * @author mohideen
 */
@RunWith(MockitoJUnitRunner.Silent.class)
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
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    private ResourceLockManager resourceLockManager;

    @Mock
    private UserTypesCache userTypesCache;

    private FedoraPropsConfig fedoraPropsConfig;

    @Before
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

    @Test(expected = TransactionNotFoundException.class)
    public void testGetTransactionWithInvalidID() {
        testTxManager.get("invalid-id");
    }

    @Test(expected = TransactionClosedException.class)
    public void testGetExpiredTransaction() throws Exception {
        testTx.expire();
        try {
            testTxManager.get(testTx.getId());
        } finally {
            // Make sure rollback is triggered
            verify(psSession).rollback();
        }
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

        assertNotNull("Continuing transaction must be present",
                testTxManager.get(continuingTx.getId()));

        testTxManager.cleanupClosedTransactions();

        // Verify that the closed transactions are stick around since they haven't expired yet
        try {
            testTxManager.get(commitTx.getId());
            fail("Transaction must be present but committed");
        } catch (final TransactionClosedException e) {
            //expected
        }
        try {
            testTxManager.get(rollbackTx.getId());
            fail("Transaction must be present but rolled back");
        } catch (final TransactionClosedException e) {
            //expected
        }

        // Force expiration of the closed transactions, rather than waiting for it
        commitTx.expire();
        rollbackTx.expire();
        testTxManager.cleanupClosedTransactions();

        // verify that closed transactions cleanedup

        verify(pssManager).removeSession(commitTx.getId());
        verify(pssManager).removeSession(rollbackTx.getId());
        verify(pssManager, never()).removeSession(continuingTx.getId());

        try {
            testTxManager.get(commitTx.getId());
            fail("Committed transaction was not cleaned up");
        } catch (final TransactionNotFoundException e) {
            //expected
        }
        try {
            testTxManager.get(rollbackTx.getId());
            fail("Rolled back transaction was not cleaned up");
        } catch (final TransactionNotFoundException e) {
            //expected
        }

        assertNotNull("Continuing transaction must be present",
                testTxManager.get(continuingTx.getId()));
    }

    // Check that the scheduled cleanup process rolls back expired transactions, but leaves
    // them around until the next cleanup call so that they can be queried.
    @Test
    public void testCleanupExpiringTransaction() throws Exception {
        fedoraPropsConfig.setSessionTimeout(Duration.ofMillis(0));

        final var expiringTx = testTxManager.create();

        Thread.sleep(100);

        testTxManager.cleanupClosedTransactions();

        try {
            testTxManager.get(expiringTx.getId());
            fail("Transaction must be expired");
        } catch (final TransactionClosedException e) {
            //expected
        }

        verify(psSession).rollback();
        verify(pssManager).removeSession(expiringTx.getId());

        testTxManager.cleanupClosedTransactions();

        try {
            testTxManager.get(expiringTx.getId());
            fail("Expired transaction was not cleaned up");
        } catch (final TransactionNotFoundException e) {
            //expected
        }
    }
}
