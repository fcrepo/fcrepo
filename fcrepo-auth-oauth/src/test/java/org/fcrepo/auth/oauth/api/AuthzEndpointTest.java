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
package org.fcrepo.auth.oauth.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.fcrepo.http.commons.session.SessionFactory;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;


public class AuthzEndpointTest {
    
    private static final String DUMMY_TOKEN = "dummyOauthToken";
    
    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private SessionFactory mockSessions;
    
    @Mock
    private Session mockSession;
    
    @Mock
    private Node mockRootNode;
    
    @Mock
    private Node mockCodeNode;
    
    private AuthzEndpoint testObj;
    
    @Before
    public void setUp() throws RepositoryException, NoSuchFieldException {
        initMocks(this);
        when(mockRootNode.getNode(startsWith("authorization-codes/")))
        .thenReturn(mockCodeNode);
        when(mockSessions.getInternalSession("oauth")).thenReturn(mockSession);
        when(mockSession.getRootNode()).thenReturn(mockRootNode);
        when(mockSession.getNode(startsWith("/authorization-codes/")))
        .thenReturn(mockCodeNode);
        when(mockRequest.getParameter("client_id")).thenReturn("bond");
        testObj = new AuthzEndpoint();
        setField(testObj, "sessions", mockSessions);
    }

    @Test
    public void testValidCodeRequest()
        throws URISyntaxException, OAuthSystemException, RepositoryException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameter("client_id")).thenReturn("bond");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getParameter("redirect_uri"))
        .thenReturn("http://fedora.info/redirect");
        when(mockRequest.getParameter(OAuth.OAUTH_RESPONSE_TYPE))
        .thenReturn("code");
        Response actual = testObj.getAuthorization(mockRequest);
        assertEquals(302, actual.getStatus());
        URI redirect = URI.create(actual.getMetadata().getFirst("Location").toString());
        assertEquals(-1, redirect.getQuery().indexOf("error="));
        verify(mockSession).save();
        verify(mockSession).logout();
    }
    
    @Test
    public void testMissingResponseType()
        throws URISyntaxException, OAuthSystemException, RepositoryException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getParameter("redirect_uri"))
        .thenReturn("http://fedora.info/redirect");
        when(mockRequest.getParameter("client_id")).thenReturn("bond");
        Response actual = testObj.getAuthorization(mockRequest);
        assertEquals(302, actual.getStatus());
        URI redirect = URI.create(actual.getMetadata().getFirst("Location").toString());
        assertEquals(0, redirect.getQuery().indexOf("error=invalid_request"));
        verify(mockSession, times(0)).save();
    }
    
    @Test
    public void testUnsupportedResponseType()
        throws URISyntaxException, OAuthSystemException, RepositoryException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getParameter("redirect_uri"))
        .thenReturn("http://fedora.info/redirect");
        when(mockRequest.getParameter("client_id")).thenReturn("bond");
        when(mockRequest.getParameter(OAuth.OAUTH_RESPONSE_TYPE))
        .thenReturn("token");
        Response actual = testObj.getAuthorization(mockRequest);
        assertEquals(302, actual.getStatus());
        URI redirect = URI.create(actual.getMetadata().getFirst("Location").toString());
        assertEquals(0, redirect.getQuery().indexOf("error=unsupported_response_type"));
        verify(mockSession, times(0)).save();
    }
    
    @Test
    public void testMissingClientId()
        throws URISyntaxException, OAuthSystemException, RepositoryException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getParameter("redirect_uri"))
        .thenReturn("http://fedora.info/redirect");
        when(mockRequest.getParameter(OAuth.OAUTH_RESPONSE_TYPE))
        .thenReturn("token");
        Response actual = testObj.getAuthorization(mockRequest);
        assertEquals(302, actual.getStatus());
        URI redirect = URI.create(actual.getMetadata().getFirst("Location").toString());
        assertEquals(0, redirect.getQuery().indexOf("error=invalid_request"));
        verify(mockSession, times(0)).save();
    }
    
    /**
     * Redirect uri parms are optional, but the error handling
     * for a bad request is different when it is absent
     * @throws URISyntaxException
     * @throws OAuthSystemException
     * @throws RepositoryException
     */
    @Test(expected=WebApplicationException.class)
    public void testMissingRedirectUriAndClientId() throws URISyntaxException, OAuthSystemException, RepositoryException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getParameter(OAuth.OAUTH_RESPONSE_TYPE))
        .thenReturn("code");
        testObj.getAuthorization(mockRequest);
    }
}
