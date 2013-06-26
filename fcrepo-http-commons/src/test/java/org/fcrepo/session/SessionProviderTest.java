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
