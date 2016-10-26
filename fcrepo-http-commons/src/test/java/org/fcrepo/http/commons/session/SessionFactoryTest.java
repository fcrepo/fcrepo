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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.SessionMissingException;
import org.fcrepo.kernel.api.services.BatchService;
import org.fcrepo.kernel.api.services.CredentialsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.api.ServletCredentials;

import com.google.common.base.Throwables;

/**
 * <p>SessionFactoryTest class.</p>
 *
 * @author awoods
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionFactoryTest {

    SessionFactory testObj;

    @Mock
    private FedoraSession txSession;

    @Mock
    private FedoraSession mockSession;

    @Mock
    private FedoraRepository mockRepo;

    @Mock
    private BatchService mockTxService;

    @Mock
    private CredentialsService mockCredService;

    @Mock
    private FedoraSession mockTx;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private Principal mockUser;

    @Before
    public void setUp() {
        testObj = new SessionFactory(mockRepo, mockTxService);
        setField(testObj, "credentialsService", mockCredService);
        testObj.init();
    }

    @Test
    public void testGetSessionWithNullPath() {
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getContextPath()).thenReturn("");
        when(mockRepo.login(any(Credentials.class))).thenReturn(mockSession);
        testObj.getSession(mockRequest);
        verify(mockRepo).login(any(ServletCredentials.class));
    }

   @Test
    public void testGetSessionUnauthenticated() {
        testObj.getInternalSession();
        verify(mockRepo).login();
    }

    @Test
    public void testCreateSession() {
        when(mockRequest.getPathInfo()).thenReturn("/some/path");
        testObj.createSession(mockRequest);
        verify(mockRepo).login(any(Credentials.class));
    }

    @Test
    public void testGetSessionFromTransaction() {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTxService.getSession("123", null)).thenReturn(mockTx);
        final HttpSession session = testObj.getSessionFromTransaction(mockRequest, "123");
        assertEquals(mockTx, session.getFedoraSession());
    }

    @Test
    public void testGetSessionThrowException() {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTxService.getSession("123", null)).thenThrow(new SessionMissingException(""));
        try {
            testObj.getSession(mockRequest);
        } catch (final RuntimeException e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            assertTrue("TransactionMissionException expected",
                    rootCause instanceof SessionMissingException);
        }
    }

    @Test
    public void testGetEmbeddedIdTx() {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        final String txId = testObj.getEmbeddedId(mockRequest, SessionFactory.Prefix.TX);
        assertEquals("txId should be 123", "123", txId);
    }

}
