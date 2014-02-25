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
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.fcrepo.auth.oauth.Constants.CLIENT_PROPERTY;
import static org.fcrepo.auth.oauth.Constants.PRINCIPAL_PROPERTY;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.fcrepo.auth.oauth.TokenRequestValidations;
import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TokenEndpointTest {

    private static final String AUTH_CODES = "/authorization-codes";

    private final static String DUMMY_AUTH_CODE = "mockAuthorizationCode";

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    TokenRequestValidations mockValidations;

    @Mock
    SessionFactory mockSessions;

    @Mock
    Session mockSession;

    @Mock
    Node mockTokenNode;

    @Mock
    Node mockTokenRootNode;

    @Mock
    Node mockRootNode;

    private TokenEndpoint testObj;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        testObj = new TokenEndpoint();
        setField(testObj, "requestValidator", mockValidations);
        setField(testObj, "sessions", mockSessions);
        when(mockSessions.getInternalSession("oauth")).thenReturn(mockSession);
        when(mockSession.getRootNode()).thenReturn(mockRootNode);
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getContentType())
        .thenReturn("application/x-www-form-urlencoded");
        when(mockRequest.getParameter("client_secret")).thenReturn("top");
        when(mockRequest.getParameter("client_id")).thenReturn("bond");
        when(mockTokenRootNode.getParent()).thenReturn(mockRootNode);
        when(mockTokenRootNode.getPath()).thenReturn(AUTH_CODES);
        when(mockTokenNode.getParent()).thenReturn(mockTokenRootNode);
        when(mockRootNode.getNode(startsWith("tokens/")))
        .thenReturn(mockTokenNode);
        when(mockSession.getNode(startsWith("/tokens/")))
        .thenReturn(mockTokenNode);
    }

    private void initForAuthCode() throws RepositoryException {
        final String authCodePath = (AUTH_CODES + "/" + DUMMY_AUTH_CODE);
        final Node mockAuthCode = mock(Node.class);
        when(mockAuthCode.getPath()).thenReturn(authCodePath);
        when(mockRootNode.getNode(authCodePath.substring(1)))
        .thenReturn(mockAuthCode);
        final Property mockProp = mock(Property.class);
        when(mockProp.getString()).thenReturn("bond");
        when(mockAuthCode.getProperty("oauth-client")).thenReturn(mockProp);
        when(mockRequest.getParameter(OAuth.OAUTH_GRANT_TYPE))
        .thenReturn(GrantType.AUTHORIZATION_CODE.toString());
        when(mockRequest.getParameter("redirect_uri"))
        .thenReturn("http://fedora.info/redirect");
        when(mockRequest.getParameter("code"))
        .thenReturn(DUMMY_AUTH_CODE);
        when(mockSession.getNode(authCodePath))
        .thenReturn(mockAuthCode);
    }

    private void initForPassword() {
        when(mockRequest.getParameter(OAuth.OAUTH_GRANT_TYPE))
        .thenReturn(GrantType.PASSWORD.toString());
        when(mockRequest.getParameter(OAuth.OAUTH_USERNAME))
        .thenReturn("testUser");
        when(mockRequest.getParameter(OAuth.OAUTH_PASSWORD))
        .thenReturn("testPassword");
    }

    private void initForRefreshToken() {
        when(mockRequest.getParameter(OAuth.OAUTH_GRANT_TYPE))
        .thenReturn(GrantType.REFRESH_TOKEN.toString());
        when(mockRequest.getParameter(OAuth.OAUTH_REFRESH_TOKEN))
        .thenReturn("dummyData");
    }

    @Test
    public void testValidRequest()
        throws OAuthSystemException, RepositoryException {
        initForAuthCode();
        when(mockValidations.isValidAuthCode(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidClient(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidSecret(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidCredentials(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        final Response actual = testObj.getToken(mockRequest);
        if (actual.getStatus() != 200) {
            System.out.println(actual.getEntity());
        }
        assertEquals(200, actual.getStatus());
        //verify the token interactions with the repo
        verify(mockTokenNode).setProperty(eq(CLIENT_PROPERTY), any(String.class));
        verify(mockTokenNode).setProperty(eq(PRINCIPAL_PROPERTY), any(String.class));
        verify(mockSession).save();
        verify(mockSession).logout();
    }

    @Test
    public void testBadClient()
        throws RepositoryException, OAuthSystemException {
        initForAuthCode();
        when(mockValidations.isValidAuthCode(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidClient(any(OAuthTokenRequest.class)))
        .thenReturn(false);
        when(mockValidations.isValidSecret(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidCredentials(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        final Response actual = testObj.getToken(mockRequest);
        assertEquals(400, actual.getStatus());
        System.out.println(actual.getEntity());
        final JsonObject json = JSON.parse(actual.getEntity().toString());
        assertEquals("invalid_client", json.get("error").getAsString().value());
    }

    @Test
    public void testBadSecret()
        throws RepositoryException, OAuthSystemException {
        initForAuthCode();
        when(mockValidations.isValidAuthCode(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidClient(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidSecret(any(OAuthTokenRequest.class)))
        .thenReturn(false);
        when(mockValidations.isValidCredentials(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        final Response actual = testObj.getToken(mockRequest);
        assertEquals(401, actual.getStatus());
        System.out.println(actual.getEntity());
        final JsonObject json = JSON.parse(actual.getEntity().toString());
        assertEquals("unauthorized_client", json.get("error").getAsString().value());
    }

    @Test
    public void testBadAuthCode()
        throws RepositoryException, OAuthSystemException {
        initForAuthCode();
        when(mockValidations.isValidAuthCode(any(OAuthTokenRequest.class)))
        .thenReturn(false);
        when(mockValidations.isValidClient(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidSecret(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidCredentials(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        final Response actual = testObj.getToken(mockRequest);
        assertEquals(400, actual.getStatus());
        System.out.println(actual.getEntity());
        final JsonObject json = JSON.parse(actual.getEntity().toString());
        assertEquals("invalid_grant", json.get("error").getAsString().value());
    }

    @Test
    public void testBadPasswordCredentials()
        throws RepositoryException, OAuthSystemException {
        initForPassword();
        when(mockValidations.isValidAuthCode(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidClient(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidSecret(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidCredentials(any(OAuthTokenRequest.class)))
        .thenReturn(false);
        final Response actual = testObj.getToken(mockRequest);
        assertEquals(400, actual.getStatus());
        System.out.println(actual.getEntity());
        final JsonObject json = JSON.parse(actual.getEntity().toString());
        assertEquals("invalid_grant", json.get("error").getAsString().value());
    }

    @Test
    public void testRefreshNotSupported()
        throws OAuthSystemException, RepositoryException {
        initForRefreshToken();
        when(mockValidations.isValidClient(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        when(mockValidations.isValidSecret(any(OAuthTokenRequest.class)))
        .thenReturn(true);
        final Response actual = testObj.getToken(mockRequest);
        assertEquals(400, actual.getStatus());
        System.out.println(actual.getEntity());
        final JsonObject json = JSON.parse(actual.getEntity().toString());
        assertEquals("invalid_grant", json.get("error").getAsString().value());
    }
}
