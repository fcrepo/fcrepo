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
package org.fcrepo.auth.oauth.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.Sets;

public class DefaultTokenRequestValidationsTest {
    
    private static String INVALID_CODE = "invalid-code";

    private static String VALID_CODE = "valid-code";

    private static String INVALID_CLIENT = "invalid-client";

    private static String VALID_CLIENT = "valid-client";

    private static String INVALID_SCOPE = "invalid-scope";

    private static String VALID_SCOPE = "valid-scope";

    @Mock OAuthTokenRequest mockRequest;
    
    @Mock
    private SessionFactory mockSessions;
    
    @Mock
    private Session mockSession;
    
    @Mock
    private Node mockNode;
    
    @Mock
    Property mockProperty;
    
    @Mock
    Property mockScopes;

    private DefaultTokenRequestValidations testObj;
    
    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockSessions.getInternalSession("oauth")).thenReturn(mockSession);
        when(mockNode.getProperty("oauth-client"))
        .thenReturn(mockProperty);
        when(mockProperty.getString()).thenReturn(VALID_CLIENT);
        when(mockSession.getNode("/authorization-codes/" + VALID_CODE))
        .thenReturn(mockNode);
        when(mockSession.getNode("/authorization-codes/" + INVALID_CODE))
        .thenThrow(new PathNotFoundException());
        when(mockSession.getNode(matches("^(\\/authorization-codes\\/)")))
        .thenThrow(new PathNotFoundException());
        when(mockScopes.getValues()).thenReturn(new Value[]{new StringValue(VALID_SCOPE)});

        testObj = new DefaultTokenRequestValidations(mockSessions);
    }
    
    @Test
    public void testValidAuthCodeUnscoped() throws RepositoryException {
        when(mockRequest.getClientId()).thenReturn(VALID_CLIENT);
        when(mockRequest.getCode()).thenReturn(VALID_CODE);
        
        assertTrue(testObj.isValidAuthCode(mockRequest));
    }
    
    @Test
    public void testValidAuthCodeInScope() throws RepositoryException {
        when(mockRequest.getClientId()).thenReturn(VALID_CLIENT);
        when(mockRequest.getCode()).thenReturn(VALID_CODE);
        
        when(mockRequest.getScopes()).thenReturn(Sets.newHashSet(VALID_SCOPE));
        when(mockNode.getProperty("oauth-scopes"))
        .thenReturn(mockScopes);
        assertTrue(testObj.isValidAuthCode(mockRequest));
    }

    @Test
    public void testValidAuthCodeOutOfScope() throws RepositoryException {
        when(mockRequest.getClientId()).thenReturn(VALID_CLIENT);
        when(mockRequest.getCode()).thenReturn(VALID_CODE);
        when(mockRequest.getScopes()).thenReturn(Sets.newHashSet(INVALID_SCOPE));
        when(mockNode.getProperty("oauth-scopes"))
        .thenReturn(mockScopes);
        
        assertFalse(testObj.isValidAuthCode(mockRequest));
    }
    @Test
    public void testInvalidAuthCode() throws RepositoryException {
        when(mockRequest.getCode()).thenReturn(INVALID_CODE);
        assertFalse(testObj.isValidAuthCode(mockRequest));
    }

    @Test
    public void testValidClient() {
        assertTrue(testObj.isValidClient(mockRequest));
    }
    
    @Test
    public void testInvalidClient() {
        assertFalse(testObj.isValidClient(null));
    }
    
    @Test
    public void testMissingClient() throws RepositoryException {
        when(mockRequest.getClientId()).thenReturn(null);
        assertFalse(testObj.isValidAuthCode(mockRequest));
    }
    
    @Test
    public void testValidSecret() {
        assertTrue(testObj.isValidSecret(mockRequest));
    }
    
    @Test
    public void testInvalidSecret() {
        assertFalse(testObj.isValidSecret(null));
    }

    @Test
    public void testValidCredentials() {
        assertTrue(testObj.isValidCredentials(mockRequest));
    }

    @Test
    public void testInvalidCredentials() {
        assertFalse(testObj.isValidCredentials(null));
    }
}
