/**
 * Copyright 2015 DuraSpace, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.TransactionMissingException;
import org.fcrepo.kernel.api.services.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.ServletCredentials;

import com.google.common.base.Throwables;

/**
 * <p>SessionFactoryTest class.</p>
 *
 * @author awoods
 */
public class SessionFactoryTest {

    SessionFactory testObj;

    @Mock
    private Session txSession;

    @Mock
    private Session mockSession;

    @Mock
    private Repository mockRepo;

    @Mock
    private TransactionService mockTxService;

    @Mock
    private Transaction mockTx;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private Principal mockUser;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new SessionFactory(mockRepo, mockTxService);
        testObj.init();
    }

    @Test
    public void testGetSessionWithNullPath() throws RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn(null);
        when(mockRequest.getContextPath()).thenReturn("");
        when(mockRepo.login(any(Credentials.class))).thenReturn(mockSession);
        testObj.getSession(mockRequest);
        verify(mockRepo).login(any(ServletCredentials.class));
    }

   @Test
    public void testGetSessionUnauthenticated() throws RepositoryException {
        testObj.getInternalSession();
        verify(mockRepo).login();
    }

    @Test
    public void testCreateSession() throws RepositoryException {
        when(mockRequest.getPathInfo()).thenReturn("/some/path");
        testObj.createSession(mockRequest);
        verify(mockRepo).login(any(Credentials.class));
    }

    @Test
    public void testGetSessionFromTransaction() {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTx.getSession()).thenReturn(mock(Session.class));
        when(mockTxService.getTransaction("123", null)).thenReturn(mockTx);
        final Session session = testObj.getSessionFromTransaction(mockRequest, "123");
        assertEquals(mockTx.getSession(), session);
    }

    @Test
    public void testGetSessionThrowException() {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        when(mockTx.getSession()).thenReturn(mock(Session.class));
        when(mockTxService.getTransaction("123", null)).thenThrow(
                new TransactionMissingException(""));
        try {
            testObj.getSession(mockRequest);
        } catch (final RuntimeException e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            assertTrue("TransactionMissionException expected",
                    rootCause instanceof TransactionMissingException);
        }
    }

    @Test
    public void testGetEmbeddedIdTx() {
        when(mockRequest.getPathInfo()).thenReturn("/tx:123/some/path");
        final String txId = testObj.getEmbeddedId(mockRequest, SessionFactory.Prefix.TX);
        assertEquals("txId should be 123", "123", txId);
    }

}
