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
package org.fcrepo.http.api;

import static java.time.Instant.now;
import static java.util.Optional.of;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;
import java.security.Principal;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.services.BatchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * <p>FedoraTransactionsTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraTransactionsTest {

    private static final String USER_NAME = "test";

    private FedoraTransactions testObj;

    private HttpSession testSession;

    @Mock
    private FedoraSession mockSession;

    @Mock
    private FedoraSession regularSession;

    @Mock
    private BatchService mockTxService;

    @Mock
    private Principal mockPrincipal;

    @Mock
    private SecurityContext mockSecurityContext;

    @Before
    public void setUp() {
        testObj = new FedoraTransactions();
        testSession = new HttpSession(mockSession);
        testSession.makeBatchSession();
        when(mockSession.getId()).thenReturn("123");
        when(mockSession.getExpires()).thenReturn(of(now().plusSeconds(100)));
        when(regularSession.getExpires()).thenReturn(of(now().minusSeconds(100)));
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", testSession);
        setField(testObj, "batchService", mockTxService);
        setField(testObj, "securityContext", mockSecurityContext);
    }

    @Test
    public void shouldStartANewTransaction() throws URISyntaxException {
        setField(testObj, "session", new HttpSession(regularSession));
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(USER_NAME);
        testObj.createTransaction(null);
        verify(mockTxService).begin(regularSession, USER_NAME);
    }

    @Test
    public void shouldUpdateExpiryOnExistingTransaction() throws URISyntaxException {
        when(mockTxService.exists(any(String.class), any(String.class))).thenReturn(true);
        testObj.createTransaction(null);
        verify(mockTxService).refresh(any(String.class), any(String.class));
    }

    @Test
    public void shouldCommitATransaction() {
        when(mockTxService.exists(any(String.class), any(String.class))).thenReturn(true);
        testObj.commit(null);
        verify(mockTxService).commit("123", null);
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransaction() {
        setField(testObj, "session", new HttpSession(regularSession));
        final Response commit = testObj.commit(null);
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfCommitIsNotCalledAtTheRepoRoot() {
        setField(testObj, "session", new HttpSession(regularSession));
        final Response commit = testObj.commit("a");
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldRollBackATransaction() {
        when(mockTxService.exists(any(String.class), any(String.class))).thenReturn(true);
        testObj.commit(null);
        verify(mockTxService).commit("123", null);
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransactionAtRollback() {
        setField(testObj, "session", new HttpSession(regularSession));
        final Response commit = testObj.rollback(null);
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfRollbackIsNotCalledAtTheRepoRoot() {
        setField(testObj, "session", testSession);
        final Response commit = testObj.rollback("a");
        assertEquals(400, commit.getStatus());
    }
}
