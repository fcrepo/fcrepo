
package org.fcrepo.session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SessionFactoryTest {

    SessionFactory testObj;

    Repository mockRepo;

    @Before
    public void setUp() {
        mockRepo = mock(Repository.class);
        testObj = new SessionFactory();
        testObj.setRepository(mockRepo);
        testObj.init();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetSessionWithNullPath() throws LoginException,
        RepositoryException {
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getPathInfo()).thenReturn(null);

        testObj.getSession(mockRequest);
        verify(mockRepo).login();
    }

    @Test
    public void testGetSessionAuthenticated() throws LoginException,
        RepositoryException {
        final SecurityContext mockContext = mock(SecurityContext.class);
        final Principal mockUser = mock(Principal.class);
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
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getPathInfo()).thenReturn(
                "/workspace:some-workspace/some/path");

        testObj.getSession(mockRequest);
        verify(mockRepo).login("some-workspace");
    }

    @Test
    public void testGetSessionWithTransaction() throws LoginException,
        RepositoryException {
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");

        TransactionService mockTxService = mock(TransactionService.class);
        testObj.setTransactionService(mockTxService);
        Transaction mockTx = mock(Transaction.class);
        when(mockTx.getSession()).thenReturn(mock(Session.class));
        when(mockTxService.getTransaction("123")).thenReturn(mockTx);

        final Session session = testObj.getSession(mockRequest);
        assertEquals(mockTx.getSession(), session);
    }

    @Test
    public void testGetAuthenticatedSessionWithWorkspace()
        throws LoginException, RepositoryException {

        final SecurityContext mockContext = mock(SecurityContext.class);
        final Principal mockUser = mock(Principal.class);
        when(mockContext.getUserPrincipal()).thenReturn(mockUser);

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getPathInfo()).thenReturn(
                "/workspace:some-workspace/some/path");

        testObj.getSession(mockContext, mockRequest);
        verify(mockRepo).login(any(Credentials.class), eq("some-workspace"));
    }

    @Test
    public void testGetAuthenticatedSessionWithTransaction()
        throws LoginException, RepositoryException {

        final SecurityContext mockContext = mock(SecurityContext.class);
        final Principal mockUser = mock(Principal.class);
        when(mockContext.getUserPrincipal()).thenReturn(mockUser);

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");

        TransactionService mockTxService = mock(TransactionService.class);
        testObj.setTransactionService(mockTxService);
        Transaction mockTx = mock(Transaction.class);
        final Session txSession = mock(Session.class);
        final Session mockSession = mock(Session.class);
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
        final SecurityContext mockContext = mock(SecurityContext.class);
        final Principal mockUser = mock(Principal.class);
        when(mockContext.getUserPrincipal()).thenReturn(mockUser);
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        testObj.getSessionProvider(mockContext, mockRequest);
    }
}
