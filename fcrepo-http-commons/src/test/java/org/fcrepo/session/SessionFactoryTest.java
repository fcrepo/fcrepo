
package org.fcrepo.session;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

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
    public void testGetSessionAuthenticated() throws LoginException,
            RepositoryException {
        final SecurityContext mockContext = mock(SecurityContext.class);
        final Principal mockUser = mock(Principal.class);
        when(mockContext.getUserPrincipal()).thenReturn(mockUser);
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        testObj.getSession(mockContext, mockRequest);
        verify(mockRepo).login(any(Credentials.class));
    }

    @Test
    public void testGetSessionUnauthenticated() throws LoginException,
            RepositoryException {
        testObj.getSession();
        verify(mockRepo).login();
    }
}
