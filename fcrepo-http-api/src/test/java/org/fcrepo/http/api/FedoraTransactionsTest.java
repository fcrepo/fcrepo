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

package org.fcrepo.http.api;

import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.fcrepo.http.api.FedoraTransactions;
import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.TxAwareSession;
import org.fcrepo.kernel.services.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class FedoraTransactionsTest {

    private FedoraTransactions testObj;

    private Session mockSession;

    @Mock
    private Session regularSession;

    @Mock
    private Transaction mockTx;

    @Mock
    private TransactionService mockTxService;

    @Before
    public void setUp() throws RepositoryException, NoSuchFieldException {
        initMocks(this);
        testObj = new FedoraTransactions();
        mockSession = TxAwareSession.newInstance(mockSession(testObj), "123");
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", mockSession);
        setField(testObj, "txService", mockTxService);

    }

    @Test
    public void shouldStartANewTransaction() throws RepositoryException,
            NoSuchFieldException {
        setField(testObj, "session", regularSession);
        when(mockTxService.beginTransaction(regularSession)).thenReturn(mockTx);
        testObj.createTransaction(createPathList());
        verify(mockTxService).beginTransaction(regularSession);
    }

    @Test
    public void shouldUpdateExpiryOnExistingTransaction()
            throws RepositoryException {
        when(mockTxService.getTransaction("123")).thenReturn(mockTx);
        testObj.createTransaction(createPathList());
        verify(mockTx).updateExpiryDate();
    }

    @Test
    public void shouldCommitATransaction() throws RepositoryException {
        testObj.commit(createPathList());
        verify(mockTxService).commit("123");
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransaction()
            throws RepositoryException, NoSuchFieldException {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.commit(createPathList());
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfCommitIsNotCalledAtTheRepoRoot()
            throws RepositoryException, NoSuchFieldException {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.commit(createPathList("a"));
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldRollBackATransaction() throws RepositoryException {
        testObj.commit(createPathList());
        verify(mockTxService).commit("123");
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransactionAtRollback()
            throws RepositoryException, NoSuchFieldException {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.rollback(createPathList());
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfRollbackIsNotCalledAtTheRepoRoot()
            throws RepositoryException, NoSuchFieldException {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.rollback(createPathList("a"));
        assertEquals(400, commit.getStatus());
    }
}
