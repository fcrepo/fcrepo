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

package org.fcrepo.session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.fcrepo.Transaction;
import org.fcrepo.services.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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
    private SecurityContext mockContext;

    @Mock
    private Principal mockUser;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new SessionFactory(mockRepo, mockTxService);
        testObj.init();
    }

    @Test
    public void testGetSessionWithNullPath() throws LoginException,
            RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn(null);
        testObj.getSession(mockRequest);
        verify(mockRepo).login();
    }

    @Test
    public void testGetSessionAuthenticated() throws LoginException,
            RepositoryException {
        when(mockContext.getUserPrincipal()).thenReturn(mockUser);
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getPathInfo()).thenReturn("/some/path");
        testObj.getSession(mockContext, mockRequest);
        verify(mockRepo).login(any(Credentials.class));
    }

    @Test
    public void testGetSessionUnauthenticated() throws LoginException,
            RepositoryException {
        testObj.getSession();
        verify(mockRepo).login();
    }

    @Test
    public void testGetSessionWithWorkspace() throws LoginException,
            RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn(
                "/workspace:some-workspace/some/path");
        testObj.getSession(mockRequest);
        verify(mockRepo).login("some-workspace");
    }

    @Test
    public void testGetSessionWithTransaction() throws LoginException,
            RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTx.getSession()).thenReturn(mock(Session.class));
        when(mockTxService.getTransaction("123")).thenReturn(mockTx);
        final Session session = testObj.getSession(mockRequest);
        assertEquals(mockTx.getSession(), session);
    }

    @Test
    public void testGetAuthenticatedSessionWithWorkspace()
            throws LoginException, RepositoryException {

        when(mockContext.getUserPrincipal()).thenReturn(mockUser);
        when(mockRequest.getPathInfo()).thenReturn(
                "/workspace:some-workspace/some/path");
        testObj.getSession(mockContext, mockRequest);
        verify(mockRepo).login(any(Credentials.class), eq("some-workspace"));
    }

    @Test
    public void testGetAuthenticatedSessionWithTransaction()
            throws LoginException, RepositoryException {

        when(mockContext.getUserPrincipal()).thenReturn(mockUser);
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(txSession.impersonate(any(Credentials.class))).thenReturn(
                mockSession);
        when(mockTx.getSession()).thenReturn(txSession);
        when(mockTxService.getTransaction("123")).thenReturn(mockTx);

        final Session session = testObj.getSession(mockContext, mockRequest);
        assertEquals(mockSession, session);
        verify(txSession).impersonate(any(Credentials.class));
    }

    @Test
    public void testGetSessionProvider() {
        when(mockContext.getUserPrincipal()).thenReturn(mockUser);
        testObj.getSessionProvider(mockContext, mockRequest);
    }
}
