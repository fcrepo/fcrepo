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
package org.fcrepo.auth.oauth;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthRuntimeException;
import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DefaultOAuthResourceProviderTest {

    private static final String DUMMY_RSID = "dummy-rsid";
    private static final String INVALID_TOKEN = "invalid-token";

    private static final String VALID_TOKEN = "valid-token";
    @Mock
    private SessionFactory mockSessions;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private Property mockClientProperty;

    @Mock
    private Property mockPrincipalProperty;

    @Mock
    private HttpServletRequest mockRequest;

    DefaultOAuthResourceProvider testObj;

    @Before
    public void setUp() throws RepositoryException, NoSuchFieldException {
        initMocks(this);
        when(mockSessions.getInternalSession("oauth")).thenReturn(mockSession);
        when(mockSession.itemExists("/tokens/" + INVALID_TOKEN))
        .thenReturn(false);
        when(mockSession.itemExists("/tokens/" + VALID_TOKEN))
        .thenReturn(true);
        when(mockClientProperty.getString()).thenReturn("dummy-client");
        when(mockPrincipalProperty.getString()).thenReturn("dummy-principal");
        when(mockNode.getProperty("oauth-client"))
        .thenReturn(mockClientProperty);
        when(mockNode.getProperty("oauth-principal"))
        .thenReturn(mockPrincipalProperty);
        when(mockNode.getPath()).thenReturn("/tokens/" + VALID_TOKEN);
        testObj = new DefaultOAuthResourceProvider();
        setField(testObj, "sessionFactory", mockSessions);
    }

    @Test
    public void testAcceptsExistingTokenRequest()
        throws PathNotFoundException, RepositoryException, OAuthProblemException {
        when(mockSession.getNode("/tokens/" + VALID_TOKEN))
        .thenReturn(mockNode);
        testObj.validateRequest(DUMMY_RSID, VALID_TOKEN, mockRequest);
        verify(mockSession).logout();
    }

    @Test(expected=OAuthRuntimeException.class)
    public void testRejectsNonexistentTokenRequest()
        throws PathNotFoundException, RepositoryException, OAuthProblemException {
        testObj.validateRequest(DUMMY_RSID, INVALID_TOKEN, mockRequest);
        verify(mockSession).logout();
    }

}
