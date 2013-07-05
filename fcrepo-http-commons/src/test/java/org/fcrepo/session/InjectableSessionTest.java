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
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class InjectableSessionTest {

    InjectableSession testObj;

    @Mock
    private Repository mockRepo;

    @Mock
    private Session mockSession;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private SecurityContext mockSecurityContext;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        final HttpServletRequest mockHttpServletRequest =
                mock(HttpServletRequest.class);
        when(mockSessionFactory.getSession(mockHttpServletRequest)).thenReturn(
                mockSession);
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
