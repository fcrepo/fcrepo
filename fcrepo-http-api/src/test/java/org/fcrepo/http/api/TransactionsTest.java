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
// import static org.mockito.Mockito.any;
// import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
// import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URISyntaxException;
import java.security.Principal;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * <p>TransactionsTest class.</p>
 *
 * @author awoods
 */
@Ignore // TODO fix these tests
@RunWith(MockitoJUnitRunner.Silent.class)
public class TransactionsTest {

    private static final String USER_NAME = "test";

    private Transactions testObj;

    @Mock
    private Transaction mockTransaction;

    @Mock
    private Transaction regularTransaction;

    @Mock
    private TransactionManager mockTxManager;

    @Mock
    private Principal mockPrincipal;

    @Mock
    private SecurityContext mockSecurityContext;

    @Before
    public void setUp() {
        testObj = new Transactions();
        // testSession = new HttpSession(mockTransaction);
        // testSession.makeBatchSession();
        when(mockTransaction.getId()).thenReturn("123");
        when(mockTransaction.getExpires()).thenReturn(of(now().plusSeconds(100)));
        when(regularTransaction.getExpires()).thenReturn(of(now().minusSeconds(100)));
        setField(testObj, "uriInfo", getUriInfoImpl());
        // setField(testObj, "session", testSession);
        // setField(testObj, "batchService", mockTxService);
        setField(testObj, "securityContext", mockSecurityContext);
    }

    @Test
    public void shouldStartANewTransaction() throws URISyntaxException {
        // setField(testObj, "session", new HttpSession(regularTransaction));
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(USER_NAME);
        testObj.createTransaction(null);
        // verify(mockTxService).begin(regularTransaction, USER_NAME);
    }

    @Test
    public void shouldUpdateExpiryOnExistingTransaction() throws URISyntaxException {
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(USER_NAME);
        // when(mockTxService.exists(any(String.class), any(String.class))).thenReturn(true);
        testObj.createTransaction(null);
        // verify(mockTxService).refresh(anyString(), anyString());
    }

    @Test
    public void shouldCommitATransaction() {
        // when(mockTxService.exists(any(String.class), any(String.class))).thenReturn(true);
        testObj.commit(null);
        // verify(mockTxService).commit("123", null);
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransaction() {
        // setField(testObj, "session", new HttpSession(regularTransaction));
        final Response commit = testObj.commit(null);
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfCommitIsNotCalledAtTheRepoRoot() {
        // setField(testObj, "session", new HttpSession(regularTransaction));
        final Response commit = testObj.commit("a");
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldRollBackATransaction() {
        // when(mockTxService.exists(any(String.class), any(String.class))).thenReturn(true);
        testObj.commit(null);
        // verify(mockTxService).commit("123", null);
    }

    @Test
    public void shouldErrorIfTheContextSessionIsNotATransactionAtRollback() {
        // setField(testObj, "session", new HttpSession(regularTransaction));
        final Response commit = testObj.rollback(null);
        assertEquals(400, commit.getStatus());
    }

    @Test
    public void shouldErrorIfRollbackIsNotCalledAtTheRepoRoot() {
        // setField(testObj, "session", testSession);
        final Response commit = testObj.rollback("a");
        assertEquals(400, commit.getStatus());
    }
}
