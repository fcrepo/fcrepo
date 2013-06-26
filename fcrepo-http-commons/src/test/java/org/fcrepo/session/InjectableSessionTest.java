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
        when(mockSessionFactory.getSession(mockHttpServletRequest)).thenReturn(
                mockSession);
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
