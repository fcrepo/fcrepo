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
package org.fcrepo.http.api;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;
import java.security.Principal;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TxSession;
import org.fcrepo.kernel.api.services.TransactionService;
import org.fcrepo.kernel.modeshape.TxAwareSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * <p>FedoraTransactionsTest class.</p>
 *
 * @author awoods
 */
public class FedoraTransactionsTest {

    private static final String USER_NAME = "test";

    private FedoraTransactions testObj;

    private Session mockSession;

    @Mock
    private Session regularSession;

    @Mock
    private Transaction mockTx;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private TransactionService mockTxService;

    @Mock
    private Principal mockPrincipal;

    @Mock
    private Workspace mockWorkspace;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new FedoraTransactions();
        final Session session = mockSession(testObj);
        when(regularSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getName()).thenReturn("default");
        mockSession = TxAwareSession.newInstance(session, "123");
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", mockSession);
        setField(testObj, "txService", mockTxService);

    }

    @Test
    public void shouldStartANewTransaction() throws URISyntaxException {
        setField(testObj, "session", regularSession);
        when(mockTxService.beginTransaction(regularSession, USER_NAME)).thenReturn(mockTx);
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(USER_NAME);
        testObj.createTransaction(null, mockRequest);
        verify(mockTxService).beginTransaction(regularSession, USER_NAME);
    }

    @Test
    public void shouldUpdateExpiryOnExistingTransaction() throws URISyntaxException {
        when(mockTxService.getTransaction(Mockito.any(TxSession.class))).thenReturn(mockTx);
        testObj.createTransaction(null, mockRequest);
        verify(mockTx).updateExpiryDate();
    }

    @Test
    public void shouldCommitATransaction() {
        testObj.commit(null);
        verify(mockTxService).commit("123");
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransaction() {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.commit(null);
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfCommitIsNotCalledAtTheRepoRoot() {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.commit("a");
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldRollBackATransaction() {
        testObj.commit(null);
        verify(mockTxService).commit("123");
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransactionAtRollback() {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.rollback(null);
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfRollbackIsNotCalledAtTheRepoRoot() {
        setField(testObj, "session", regularSession);
        final Response commit = testObj.rollback("a");
        assertEquals(400, commit.getStatus());
    }
}
