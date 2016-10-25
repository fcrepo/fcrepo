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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.FedoraSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test the HttpSession class
 * @author acoburn
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpSessionTest {

    private String SESSION_ID = "session-id";

    @Mock
    private FedoraSession mockSession;

    @Before
    public void setUp() {
        when(mockSession.getId()).thenReturn(SESSION_ID);
    }

    @Test
    public void testHttpSession() {
        final HttpSession session = new HttpSession(mockSession);
        assertFalse(session.isBatchSession());
        session.makeBatchSession();
        assertTrue(session.isBatchSession());
        assertEquals(SESSION_ID, session.getId());
        assertEquals(mockSession, session.getFedoraSession());
    }
}
