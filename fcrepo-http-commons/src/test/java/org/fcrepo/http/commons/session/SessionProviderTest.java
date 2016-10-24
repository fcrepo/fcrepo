/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.fcrepo.kernel.api.FedoraSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * <p>SessionProviderTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionProviderTest {

    SessionProvider testObj;

    @Mock
    private FedoraSession mockSession;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Before
    public void setUp() {
        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockSessionFactory.getSession(mockHttpServletRequest)).thenReturn(mockSession);
        testObj = new SessionProvider(mockHttpServletRequest);
        setField(testObj, "sessionFactory", mockSessionFactory);
        setField(testObj, "request", mockHttpServletRequest);

    }

    @Test
    public void testProvide() {
        final FedoraSession inj = testObj.provide();
        assertNotNull("Didn't get a session", inj);
    }
}
