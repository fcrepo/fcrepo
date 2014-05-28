/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.http.commons.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.lang.reflect.Proxy;
import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.fcrepo.kernel.impl.LockReleasingSession;
import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;
import org.fcrepo.kernel.services.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.ServletCredentials;

import com.google.common.base.Throwables;

/**
 * <p>SessionFactoryTest class.</p>
 *
 * @author awoods
 */
public class SessionFactoryTest {

    SessionFactory testObj;

    @Mock
    private Session txSession;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepo;

    @Mock
    private TransactionService mockTxService;

    @Mock
    private Transaction mockTx;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private Principal mockUser;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new SessionFactory(mockRepo, mockTxService);
        testObj.init();
    }

    @Test
    public void testGetSessionWithNullPath() throws RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRepo.login(any(Credentials.class))).thenReturn(mockSession);
        testObj.getSession(mockRequest);
        verify(mockRepo).login(any(ServletCredentials.class));
    }

   @Test
    public void testGetSessionUnauthenticated() throws RepositoryException {
        testObj.getInternalSession();
        verify(mockRepo).login();
    }

    @Test
    public void testGetSessionUnauthenticatedWithWorkspace()
            throws RepositoryException {
        testObj.getInternalSession("ws");
        verify(mockRepo).login(eq("ws"));
    }

    @Test
    public void testCreateSession() throws RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn("/some/path");
        testObj.createSession(mockRequest);
        verify(mockRepo).login(any(Credentials.class));
    }

    @Test
    public void testCreateSessionWithWorkspace() throws RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn(
                "/workspace:some-workspace/some/path");
        testObj.createSession(mockRequest);
        verify(mockRepo).login(any(ServletCredentials.class), eq("some-workspace"));
    }

    @Test
    public void testGetSessionFromTransaction() throws RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTx.getSession()).thenReturn(mock(Session.class));
        when(mockTxService.getTransaction("123", null)).thenReturn(mockTx);
        final Session session = testObj.getSessionFromTransaction(mockRequest, "123");
        assertEquals(mockTx.getSession(), session);
    }

    @Test
    public void testGetSessionThrowException() throws RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTx.getSession()).thenReturn(mock(Session.class));
        when(mockTxService.getTransaction("123", null)).thenThrow(
                new TransactionMissingException(""));
        try {
            testObj.getSession(mockRequest);
        } catch (final RuntimeException e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            assertTrue("TransactionMissionException expected",
                    rootCause instanceof TransactionMissingException);
        }
    }

    @Test
    public void testGetAuthenticatedSessionWithTransaction()
            throws RepositoryException {
        final String fedoraUser = "fedoraUser";
        when(mockRequest.getUserPrincipal()).thenReturn(mockUser);
        when(mockUser.getName()).thenReturn(fedoraUser);
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTx.getSession()).thenReturn(txSession);
        when(mockTx.isAssociatedWithUser(eq(fedoraUser))).thenReturn(true);
        when(mockTxService.getTransaction("123", fedoraUser))
                .thenReturn(mockTx);
        final Session session = testObj.getSession(mockRequest);
        assertEquals(txSession, ((LockReleasingSession) Proxy.getInvocationHandler(session)).getWrappedSession());
        verify(mockTx).getSession();
    }

    @Test
    public void testGetEmbeddedIdTx() {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        final String txId = testObj.getEmbeddedId(mockRequest, SessionFactory.Prefix.TX);
        assertEquals("txId should be 123", "123", txId);
    }

    @Test
    public void testGetEmbeddedIdWorkspace() {
        when(mockRequest.getPathInfo()).thenReturn("/workspace:some-workspace/some/path");
        final String wsId = testObj.getEmbeddedId(mockRequest, SessionFactory.Prefix.WORKSPACE);
        assertEquals("wsId should be some-workspace", "some-workspace", wsId);
    }

    @Test
    public void testGetEmbeddedIdNotExisting() {
        when(mockRequest.getPathInfo()).thenReturn("/some/path");
        final String wsId = testObj.getEmbeddedId(mockRequest, SessionFactory.Prefix.WORKSPACE);
        assertNull("expected wsId to be null", wsId);
    }

    @Test
    public void testGetEmbeddedIdWithEmptyPath() {
        when(mockRequest.getPathInfo()).thenReturn("");
        final String wsId = testObj.getEmbeddedId(mockRequest, SessionFactory.Prefix.WORKSPACE);
        assertNull("expected wsId to be null", wsId);
    }

    @Test
    public void testGetEmbeddedIdWithNullPath() {
        when(mockRequest.getPathInfo()).thenReturn(null);
        final String wsId = testObj.getEmbeddedId(mockRequest, SessionFactory.Prefix.WORKSPACE);
        assertNull("expected wsId to be null", wsId);
    }



}
