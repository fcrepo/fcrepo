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
package org.fcrepo.auth.oauth.filter;

import static org.apache.oltu.oauth2.common.OAuth.OAUTH_CLIENT_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rsfilter.OAuthClient;
import org.apache.oltu.oauth2.rsfilter.OAuthDecision;
import org.apache.oltu.oauth2.rsfilter.OAuthRSProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


public class OAuthFilterTest {

    private static final String DUMMY_REALM = "DUMMY-REALM";
    private static final String DUMMY_TOKEN = "DUMMY-TOKEN";

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @Mock
    private OAuthRSProvider mockProvider;

    @Mock
    private OAuthDecision mockDecision;

    @Mock
    private OAuthClient mockClient;

    private OAuthFilter testObj;

    @Before
    public void setUp(){
        initMocks(this);
        testObj = new OAuthFilter();
        testObj.setProvider(mockProvider);
        testObj.setRealm(DUMMY_REALM);
    }

    @Test
    public void testValidRequest()
        throws IOException, ServletException, OAuthProblemException {
        when(mockRequest.getRequestURI()).thenReturn("foo://bar");
        /**
         *  the default validation method is via a header
         *  of the format "Authorization: Bearer $TOKEN"
         */
        when(mockRequest.getHeader(OAuth.HeaderType.AUTHORIZATION))
            .thenReturn("Bearer " + DUMMY_TOKEN);
        when(mockProvider.validateRequest(DUMMY_REALM, DUMMY_TOKEN, mockRequest))
            .thenReturn(mockDecision);
        when(mockDecision.getOAuthClient()).thenReturn(mockClient);
        when(mockProvider.validateRequest(
            eq(DUMMY_REALM), any(String.class), eq(mockRequest)))
            .thenReturn(mockDecision);
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // a successful request should set the client id attribute
        verify(mockRequest).setAttribute(eq(OAUTH_CLIENT_ID), any(String.class));
        // a successful request should not send an error back to client
        verify(mockResponse, times(0)).sendError(any(Integer.class));
        // a successful request should call the rest of the filter chain
        verify(mockFilterChain)
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }


    /**
     * The token-bearing header is expected to be of the format
     * Authorization : Bearer $TOKEN
     * Our OAuth library sends no elaborating error type back
     * in this circumstance, resulting in a generic 401 (Unauthorized)
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testBadAuthorizationHeader() throws IOException, ServletException {
        when(mockRequest.getRequestURI()).thenReturn("foo://bar");
        when(mockRequest.getHeader(OAuth.HeaderType.AUTHORIZATION))
        .thenReturn("Broken " + DUMMY_TOKEN);
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // a failed request should not set the client id attribute
        verify(mockRequest, times(0)).setAttribute(eq(OAUTH_CLIENT_ID), any(String.class));
        // a failed request should send an error response
        verify(mockResponse).sendError(401);
        // a failed request should not call the rest of the filter chain
        verify(mockFilterChain, times(0))
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }

    /**
     * The token-bearing header is must be present for a default
     * configuration. Our OAuth library sends no elaborating error type back
     * when it is missing, resulting in a generic 401 (Unauthorized)
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testMissingAuthorizationHeader()
        throws IOException, ServletException {
        when(mockRequest.getRequestURI()).thenReturn("foo://bar");
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // a failed request should not set the client id attribute
        verify(mockRequest, times(0)).setAttribute(eq(OAUTH_CLIENT_ID), any(String.class));
        // a failed request should send an error back to client
        verify(mockResponse).sendError(401);
        // a failed request should not call the rest of the filter chain
        verify(mockFilterChain, times(0))
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }

    /**
     * A misconfigured filter that attempts to parse tokens in multiple
     * ways is treated as an invalid request by our OAuth library, and
     * should result in a 400 (Bad Request)
     * @throws IOException
     * @throws ServletException
     * @throws OAuthProblemException
     */
    @Test
    public void testMultipleStyles()
        throws IOException, ServletException, OAuthProblemException {
        when(mockRequest.getRequestURI()).thenReturn("foo://bar");
        when(mockRequest.getHeader(OAuth.HeaderType.AUTHORIZATION))
        .thenReturn("Bearer " + DUMMY_TOKEN);
        when(mockRequest.getParameter(OAuth.OAUTH_BEARER_TOKEN))
        .thenReturn(DUMMY_TOKEN);
        when(mockRequest.getQueryString())
        .thenReturn("access_token=" + DUMMY_TOKEN);
        when(mockProvider.validateRequest(DUMMY_REALM, DUMMY_TOKEN, mockRequest))
        .thenReturn(mockDecision);
        when(mockDecision.getOAuthClient()).thenReturn(mockClient);
        final HashSet<ParameterStyle> styles = new HashSet<>(2);
        styles.add(ParameterStyle.HEADER);
        styles.add(ParameterStyle.QUERY);
        testObj.setParameterStyles(styles);
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // a failed request should not set the client id attribute
        verify(mockRequest, times(0)).setAttribute(eq(OAUTH_CLIENT_ID), any(String.class));
        // a failed request should send an error back to client
        verify(mockResponse).sendError(400);
        // a failed request should not call the rest of the filter chain
        verify(mockFilterChain, times(0))
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }

    /**
     * A misconfigured filter that has no token parsing strategies
     * is treated as an invalid request by our OAuth library, and
     * should result in a 400 (Bad Request)
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testNoStyles()
        throws IOException, ServletException {
        when(mockRequest.getRequestURI()).thenReturn("foo://bar");
        testObj.setParameterStyles(new HashSet<ParameterStyle>());
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // a failed request should not set the client id attribute
        verify(mockRequest, times(0)).setAttribute(eq(OAUTH_CLIENT_ID), any(String.class));
        // a failed request should send an error back to client
        verify(mockResponse).sendError(400);
        // a failed request should not call the rest of the filter chain
        verify(mockFilterChain, times(0))
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }
}
