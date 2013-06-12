
package org.fcrepo.session;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;

public class SessionProviderTest {

    SessionProvider testObj;

    Repository mockRepo;

    Session mockSession;

    @Before
    public void setUp() throws RepositoryException {
        mockSession = mock(Session.class);
        mockRepo = mock(Repository.class);
        final SessionFactory mockSessionFactory = mock(SessionFactory.class);
        mockSessionFactory.setRepository(mockRepo);
        when(mockSessionFactory.getSession()).thenReturn(mockSession);
        final SecurityContext mockSecurityContext = mock(SecurityContext.class);
        final HttpServletRequest mockHttpServletRequest =
                mock(HttpServletRequest.class);
        when(
                mockSessionFactory.getSession(mockSecurityContext,
                        mockHttpServletRequest)).thenReturn(mockSession);

        testObj = new SessionProvider();
        testObj.setSessionFactory(mockSessionFactory);
        testObj.setSecContext(mockSecurityContext);
        testObj.setRequest(mockHttpServletRequest);

    }

    @Test
    public void testGetInjectable() {
        final ComponentContext con = mock(ComponentContext.class);
        final InjectedSession in = mock(InjectedSession.class);
        final Injectable<Session> inj = testObj.getInjectable(con, in);
        assertNotNull("Didn't get an Injectable<Session>!", inj);
        assertTrue("Didn't get an InjectableSession!", InjectableSession.class
                .isAssignableFrom(inj.getClass()));
    }
}
