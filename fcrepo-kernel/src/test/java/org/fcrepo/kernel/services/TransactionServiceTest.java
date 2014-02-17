/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.services;

import static java.lang.System.currentTimeMillis;
import static org.fcrepo.kernel.Transaction.State.NEW;
import static org.fcrepo.kernel.services.TransactionService.FCREPO4_TX_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author frank asseg
 */
public class TransactionServiceTest {

    private static final String IS_A_TX = "foo";

    private static final String NOT_A_TX = "bar";

    TransactionService service;

    @Mock
    private Transaction mockTx;

    @Mock
    private Session mockSession;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        service = new TransactionService();
        when(mockTx.getId()).thenReturn(IS_A_TX);
        final Field txsField =
                TransactionService.class.getDeclaredField("transactions");
        txsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, Transaction> txs =
                (Map<String, Transaction>) txsField
                        .get(TransactionService.class);
        txs.put(IS_A_TX, mockTx);
    }

    @Test
    public void testExpiration() throws Exception {
        final Date fiveSecondsAgo = new Date(currentTimeMillis() - 5000);
        when(mockTx.getExpires()).thenReturn(fiveSecondsAgo);
        service.removeAndRollbackExpired();
        verify(mockTx).rollback();
    }

    @Test
    public void testExpirationThrowsRepositoryException() throws Exception {
        final Date fiveSecondsAgo = new Date(currentTimeMillis() - 5000);
        doThrow(new RepositoryException("")).when(mockTx).rollback();
        when(mockTx.getExpires()).thenReturn(fiveSecondsAgo);
        service.removeAndRollbackExpired();
    }

    @Test
    public void testCreateTx() throws Exception {
        final Transaction tx = service.beginTransaction(mock(Session.class), "test");
        assertNotNull(tx);
        assertNotNull(tx.getCreated());
        assertNotNull(tx.getId());
        assertTrue(tx.isAssociatedWithUser("test"));
        assertEquals(NEW, tx.getState());
    }

    @Test
    public void testGetTx() throws Exception {
        final Transaction tx = service.getTransaction(IS_A_TX);
        assertNotNull(tx);
    }

    @Test(expected = TransactionMissingException.class)
    public void testGetNonTx() throws TransactionMissingException {
        service.getTransaction(NOT_A_TX);
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
    public void testExists() throws Exception {
        assertTrue(service.exists(IS_A_TX));
        assertFalse(service.exists(NOT_A_TX));
    }

    @Test
    public void testCommitTx() throws Exception {
        final Transaction tx = service.commit(IS_A_TX);
        assertNotNull(tx);
        verify(mockTx).commit(null);
    }

    @Test(expected = RepositoryException.class)
    public void testCommitRemovedTransaction() throws Exception {
        final Transaction tx = service.commit(IS_A_TX);
        service.getTransaction(tx.getId());
    }

    @Test
    public void testRollbackTx() throws Exception {
        final Transaction tx = service.rollback(IS_A_TX);
        assertNotNull(tx);
        verify(mockTx).rollback();
    }

    @Test(expected = RepositoryException.class)
    public void testRollbackRemovedTransaction() throws Exception {
        final Transaction tx = service.rollback(IS_A_TX);
        service.getTransaction(tx.getId());
    }

    @Test(expected = RepositoryException.class)
    public void testRollbackWithNonTx() throws RepositoryException {
        service.rollback(NOT_A_TX);
    }

    @Test(expected = RepositoryException.class)
    public void testCommitWithNonTx() throws RepositoryException {
        service.commit(NOT_A_TX);
    }
}
