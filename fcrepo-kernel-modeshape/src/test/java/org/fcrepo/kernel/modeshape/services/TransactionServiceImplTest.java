/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.services;

import static java.lang.System.currentTimeMillis;
import static org.fcrepo.kernel.api.Transaction.State.NEW;
import static org.fcrepo.kernel.modeshape.services.TransactionServiceImpl.FCREPO4_TX_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TransactionMissingException;
import org.fcrepo.kernel.api.services.TransactionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author frank asseg
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    private static final String IS_A_TX = "foo";

    private static final String NOT_A_TX = "bar";

    private static final String USER_NAME = "test";

    private static final String ANOTHER_USER_NAME = "another";

    TransactionService service;

    @Mock
    private Transaction mockTx;

    @Mock
    private Session mockSession;

    @Before
    public void setup() throws Exception {
        service = new TransactionServiceImpl();
        when(mockTx.getId()).thenReturn(IS_A_TX);
        when(mockTx.isAssociatedWithUser(null)).thenReturn(true);
        final Field txsField =
                TransactionServiceImpl.class.getDeclaredField("transactions");
        txsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, Transaction> txs =
                (Map<String, Transaction>) txsField
                        .get(TransactionService.class);
        txs.put(IS_A_TX, mockTx);
    }

    @Test
    public void testExpiration() {
        final Date fiveSecondsAgo = new Date(currentTimeMillis() - 5000);
        when(mockTx.getExpires()).thenReturn(fiveSecondsAgo);
        service.removeAndRollbackExpired();
        verify(mockTx).rollback();
    }

    @Test
    public void testExpirationThrowsRepositoryException() {
        final Date fiveSecondsAgo = new Date(currentTimeMillis() - 5000);
        doThrow(new RepositoryRuntimeException("")).when(mockTx).rollback();
        when(mockTx.getExpires()).thenReturn(fiveSecondsAgo);
        service.removeAndRollbackExpired();
    }

    @Test
    public void testCreateTx() throws Exception {
        final Transaction tx = service.beginTransaction(mockSession, USER_NAME);
        assertNotNull(tx);
        assertNotNull(tx.getCreated());
        assertNotNull(tx.getId());
        assertTrue(tx.isAssociatedWithUser(USER_NAME));
        assertEquals(NEW, tx.getState());
    }

    @Test
    public void testGetTx() throws Exception {
        final Transaction tx = service.getTransaction(IS_A_TX, null);
        assertNotNull(tx);
    }

    @Test(expected = TransactionMissingException.class)
    public void testHijackingNotPossible() {
        final Transaction tx = service.beginTransaction(mockSession, USER_NAME);
        service.getTransaction(tx.getId(), ANOTHER_USER_NAME);
    }

    @Test(expected = TransactionMissingException.class)
    public void testHijackingNotPossibleWithAnonUser() {
        final Transaction tx = service.beginTransaction(mockSession, USER_NAME);
        service.getTransaction(tx.getId(), null);
    }

    @Test(expected = TransactionMissingException.class)
    public void testHijackingNotPossibleWhenStartedAnonUser() {
        final Transaction tx = service.beginTransaction(mockSession, null);
        service.getTransaction(tx.getId(), USER_NAME);
    }

    @Test(expected = TransactionMissingException.class)
    public void testGetNonTx() throws TransactionMissingException {
        service.getTransaction(NOT_A_TX, null);
    }

    @Test
    public void testGetTxForSession() throws Exception {
        when(mockSession.getNamespaceURI(FCREPO4_TX_ID)).thenReturn(IS_A_TX);
        final Transaction tx = service.getTransaction(mockSession);
        assertEquals(IS_A_TX, tx.getId());
    }

    @Test(expected = TransactionMissingException.class)
    public void testGetTxForNonTxSession() throws RepositoryException {
        when(mockSession.getNamespaceURI(FCREPO4_TX_ID)).thenThrow(new NamespaceException(""));
        service.getTransaction(mockSession);
    }

    @Test
    public void testExists() {
        assertTrue(service.exists(IS_A_TX));
        assertFalse(service.exists(NOT_A_TX));
    }

    @Test
    public void testCommitTx() {
        final Transaction tx = service.commit(IS_A_TX);
        assertNotNull(tx);
        verify(mockTx).commit();
    }

    @Test(expected = TransactionMissingException.class)
    public void testCommitRemovedTransaction() throws Exception {
        final Transaction tx = service.commit(IS_A_TX);
        service.getTransaction(tx.getId(), null);
    }

    @Test
    public void testRollbackTx() {
        final Transaction tx = service.rollback(IS_A_TX);
        assertNotNull(tx);
        verify(mockTx).rollback();
    }

    @Test(expected = TransactionMissingException.class)
    public void testRollbackRemovedTransaction() throws Exception {
        final Transaction tx = service.rollback(IS_A_TX);
        service.getTransaction(tx.getId(), null);
    }

    @Test(expected = TransactionMissingException.class)
    public void testRollbackWithNonTx() {
        service.rollback(NOT_A_TX);
    }

    @Test(expected = TransactionMissingException.class)
    public void testCommitWithNonTx() {
        service.commit(NOT_A_TX);
    }
}
