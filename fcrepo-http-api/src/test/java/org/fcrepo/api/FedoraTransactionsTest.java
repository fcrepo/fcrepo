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

package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.fcrepo.Transaction;
import org.fcrepo.TxAwareSession;
import org.fcrepo.services.TransactionService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraTransactionsTest {

    FedoraTransactions testObj;

    private Session mockSession;

    private TransactionService txService;

    @Before
    public void setUp() throws RepositoryException, NoSuchFieldException {

        testObj = new FedoraTransactions();

        mockSession = TxAwareSession.newInstance(mockSession(testObj), "123");

        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        TestHelpers.setField(testObj, "session", mockSession);

        txService = mock(TransactionService.class);
        TestHelpers.setField(testObj, "txService", txService);

    }

    @Test
    public void shouldStartANewTransaction() throws RepositoryException, NoSuchFieldException {
        Transaction mockTx = mock(Transaction.class);
        final Session regularSession = mock(Session.class);
        TestHelpers.setField(testObj, "session", regularSession);
        when(txService.beginTransaction(regularSession)).thenReturn(mockTx);
        testObj.createTransaction(createPathList());
        verify(txService).beginTransaction(regularSession);
    }

    @Test
    public void shouldUpdateExpiryOnExistingTransaction()
        throws RepositoryException {
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
    public void shouldErrorIfTheContextSessionIsNotATransaction()
        throws RepositoryException, NoSuchFieldException {
        TestHelpers.setField(testObj, "session", mock(Session.class));
        final Response commit = testObj.commit(createPathList());
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfCommitIsNotCalledAtTheRepoRoot()
        throws RepositoryException, NoSuchFieldException {
        TestHelpers.setField(testObj, "session", mock(Session.class));
        final Response commit = testObj.commit(createPathList("a"));
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldRollBackATransaction() throws RepositoryException {
        testObj.commit(createPathList());
        verify(txService).commit("123");
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransactionAtRollback()
        throws RepositoryException, NoSuchFieldException {
        TestHelpers.setField(testObj, "session", mock(Session.class));
        final Response commit = testObj.rollback(createPathList());
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfRollbackIsNotCalledAtTheRepoRoot()
        throws RepositoryException, NoSuchFieldException {
        TestHelpers.setField(testObj, "session", mock(Session.class));
        final Response commit = testObj.rollback(createPathList("a"));
        assertEquals(400, commit.getStatus());
    }
}
