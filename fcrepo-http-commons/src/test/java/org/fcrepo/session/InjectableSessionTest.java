
package org.fcrepo.session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.junit.Before;
import org.junit.Test;

public class InjectableSessionTest {

    InjectableSession testObj;

    Repository mockRepo;

    Session mockSession;

    @Before
    public void setUp() throws RepositoryException {
        final HttpServletRequest mockHttpServletRequest =
                mock(HttpServletRequest.class);
        mockSession = mock(Session.class);
        mockRepo = mock(Repository.class);
        final SessionFactory mockSessionFactory = mock(SessionFactory.class);
        mockSessionFactory.setRepository(mockRepo);
        when(mockSessionFactory.getSession(mockHttpServletRequest)).thenReturn(mockSession);
        final SecurityContext mockSecurityContext = mock(SecurityContext.class);
        when(
                mockSessionFactory.getSession(mockSecurityContext,
                        mockHttpServletRequest)).thenReturn(mockSession);
        testObj =
                new InjectableSession(mockSessionFactory, mockSecurityContext,
                        mockHttpServletRequest);

    }

    @Test
    public void testGetValue() {
        assertEquals("Didn't get the Session we expected!", mockSession,
                testObj.getValue());
    }

}
