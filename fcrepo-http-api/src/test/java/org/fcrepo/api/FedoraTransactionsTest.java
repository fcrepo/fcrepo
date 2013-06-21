package org.fcrepo.api;

import org.fcrepo.Transaction;
import org.fcrepo.TxAwareSession;
import org.fcrepo.services.TransactionService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FedoraTransactionsTest {
    FedoraTransactions testObj;
    private Session mockSession;
    private TransactionService txService;


    @Before
    public void setUp() throws RepositoryException {

        testObj = new FedoraTransactions();

        mockSession = TxAwareSession.newInstance(mockSession(testObj), "123");
        testObj.setSession(mockSession);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
        txService = mock(TransactionService.class);
        testObj.setTxService(txService);

    }

    @Test
    public void shouldStartANewTransaction() throws RepositoryException {
        Transaction mockTx = mock(Transaction.class);
        final Session regularSession = mock(Session.class);
        testObj.setSession(regularSession);
        when(txService.beginTransaction(regularSession)).thenReturn(mockTx);
        testObj.createTransaction(createPathList());
        verify(txService).beginTransaction(regularSession);
    }


    @Test
    public void shouldUpdateExpiryOnExistingTransaction() throws RepositoryException {
        Transaction mockTx = mock(Transaction.class);
        when(txService.getTransaction("123")).thenReturn(mockTx);
        testObj.createTransaction(createPathList());
        verify(mockTx).updateExpiryDate();
    }

    @Test
    public void shouldCommitATransaction() throws RepositoryException {
        testObj.commit(createPathList());
        verify(txService).commit("123");
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransaction() throws RepositoryException {
        testObj.setSession(mock(Session.class));
        final Response commit = testObj.commit(createPathList());
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfCommitIsNotCalledAtTheRepoRoot() throws RepositoryException {
        testObj.setSession(mock(Session.class));
        final Response commit = testObj.commit(createPathList("a"));
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldRollBackATransaction() throws RepositoryException {
        testObj.commit(createPathList());
        verify(txService).commit("123");
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransactionAtRollback() throws RepositoryException {
        testObj.setSession(mock(Session.class));
        final Response commit = testObj.rollback(createPathList());
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfRollbackIsNotCalledAtTheRepoRoot() throws RepositoryException {
        testObj.setSession(mock(Session.class));
        final Response commit = testObj.rollback(createPathList("a"));
        assertEquals(400, commit.getStatus());
    }
}
