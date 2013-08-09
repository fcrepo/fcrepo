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

package org.fcrepo.http.commons.session;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.spi.inject.Injectable;

public class SessionProviderTest {

    SessionProvider testObj;

    @Mock
    private Session mockSession;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private SecurityContext mockSecurityContext;

    @Mock
    private ComponentContext con;

    @Mock
    private InjectedSession in;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Before
    public void setUp() throws RepositoryException, NoSuchFieldException {
        initMocks(this);
        when(mockSessionFactory.getSession()).thenReturn(mockSession);
        when(
                mockSessionFactory.getSession(mockSecurityContext,
                        mockHttpServletRequest)).thenReturn(mockSession);
        testObj = new SessionProvider();
        setField(testObj, "sessionFactory", mockSessionFactory);
        setField(testObj, "secContext", mockSecurityContext);
        setField(testObj, "request", mockHttpServletRequest);

    }

    @Test
    public void testGetInjectable() {
        final Injectable<Session> inj = testObj.getInjectable(con, in);
        assertNotNull("Didn't get an Injectable<Session>!", inj);
        assertTrue("Didn't get an InjectableSession!", InjectableSession.class
                .isAssignableFrom(inj.getClass()));
    }
}
