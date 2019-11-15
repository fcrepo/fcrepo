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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * <p>
 * TransactionTest class.
 * </p>
 *
 * @author mohideen
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionImplTest {

    private TransactionImpl testTx;

    @Mock
    private TransactionManagerImpl txManager;

    @Mock
    private PersistentStorageSessionManager pssManager;

    @Mock
    private PersistentStorageSession psSession;

    @Before
    public void setUp() {
        when(pssManager.getSession("123")).thenReturn(psSession);
        when(txManager.getPersistentStorageSessionManager()).thenReturn(pssManager);
        testTx = new TransactionImpl("123", txManager);
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
    public void testCommit() {
        testTx.commit();
        try {
            verify(psSession).commit();
        } catch (PersistentStorageException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCommitIfShortLived() {
        testTx.setShortLived(true);
        testTx.commitIfShortLived();
        try {
            verify(psSession).commit();
        } catch (PersistentStorageException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCommitIfShortLivedOnNonShortLived() {
        testTx.setShortLived(false);
        testTx.commitIfShortLived();
        try {
            verify(psSession, never()).commit();
        } catch (PersistentStorageException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = RuntimeException.class)
    public void testCommitExpired() {
        testTx.expire();
        testTx.commit();
        try {
            verify(psSession).commit();
        } catch (PersistentStorageException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = RuntimeException.class)
    public void testCommitRolledbackTx() {
        testTx.rollback();
        testTx.commit();
        try {
            verify(psSession, never()).commit();
        } catch (PersistentStorageException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRollback() {
        testTx.rollback();
        try {
            verify(psSession).rollback();
        } catch (PersistentStorageException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = RuntimeException.class)
    public void testRollbackCommited() {
        testTx.commit();
        testTx.rollback();
        try {
            verify(psSession, never()).rollback();
        } catch (PersistentStorageException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExpire() {
        testTx.expire();
        assertEquals(true, testTx.hasExpired());
    }

    @Test
    public void testRefresh() {
        final Instant previousExpiry = testTx.getExpires();
        testTx.refresh();
        assertTrue(testTx.getExpires().isAfter(previousExpiry));
    }

    @Test
    public void testNewTransactionNotExpired() {
        assertTrue(testTx.getExpires().compareTo(Instant.now()) > 0);
    }
}
