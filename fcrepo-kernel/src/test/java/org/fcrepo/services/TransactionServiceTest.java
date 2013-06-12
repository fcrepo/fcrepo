/**
 *
 */
package org.fcrepo.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Transaction;
import org.fcrepo.Transaction.State;
import org.junit.Before;
import org.junit.Test;


/**
 * @author frank asseg
 *
 */
public class TransactionServiceTest {
    private static final String IS_A_TX = "foo";
    private static final String NOT_A_TX = "bar";

    TransactionService service;

    Session mockSession;

    Transaction mockTx;

    @Before
    public void setup() throws Exception {
        service = new TransactionService();
        mockTx = mock(Transaction.class);
        when(mockTx.getId()).thenReturn(IS_A_TX);
        Field txsField = TransactionService.class.getDeclaredField("TRANSACTIONS");
        txsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Transaction> txs = (Map<String, Transaction>) txsField.get(TransactionService.class);
        txs.put(IS_A_TX, mockTx);
    }

    @Test
    public void testExpiration() throws Exception {
        Date fiveSecondsAgo = new Date(System.currentTimeMillis() - 5000);
        when(mockTx.getExpires()).thenReturn(fiveSecondsAgo);
        service.removeAndRollbackExpired();
        verify(mockTx).rollback();
    }

    @Test
    public void testCreateTx() throws Exception {
        Transaction tx = service.beginTransaction(null);
        assertNotNull(tx);
        assertNotNull(tx.getCreated());
        assertNotNull(tx.getId());
        assertTrue(tx.getState() == State.NEW);
    }

    @Test
    public void testGetTx() throws Exception {
        Transaction tx = service.getTransaction(IS_A_TX);

        assertNotNull(tx);

        try{
            tx = service.getTransaction(NOT_A_TX);
            fail("Transaction retrieved for nonexistent id " + NOT_A_TX);
        }catch(RepositoryException e){
            // just checking that the exception occurs
        }
    }

    @Test
    public void testCommitTx() throws Exception {
        Transaction tx = service.commit(IS_A_TX);

        assertNotNull(tx);
        verify(mockTx).commit();
        try{
            tx = service.getTransaction(tx.getId());
            fail("Transaction is available after commit");
        }catch(RepositoryException e){
            // just checking that the exception occurs
        }
    }

    @Test
    public void testRollbackTx() throws Exception {
        Transaction tx = service.rollback(IS_A_TX);

        assertNotNull(tx);
        verify(mockTx).rollback();
        try{
            tx = service.getTransaction(tx.getId());
            fail("Transaction is available after commit");
        }catch(RepositoryException e){
            // just checking that the exception occurs
        }
    }
}
