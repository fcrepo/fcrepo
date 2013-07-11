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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


public class RestrictToAuthNFilterTest {

    private static final String AUTHN_REQUIRED_URI =
            "foo://bar/authenticated/stuff";
    private static final String NO_AUTH_REQUIRED_URI =
            "foo://bar/unauthenticated/stuff";

    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private FilterChain mockFilterChain;
    
    @Before
    public void setUp() {
        initMocks(this);
    }
    
    @Test
    public void testValidRequestAuthRequired()
        throws IOException, ServletException, OAuthProblemException {
        RestrictToAuthNFilter testObj = new RestrictToAuthNFilter();
        when(mockRequest.getRequestURI()).thenReturn(AUTHN_REQUIRED_URI);
        Principal mockPrincipal = mock(Principal.class);
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // a successful request should not send an error back to client
        verify(mockResponse, times(0)).sendError(any(Integer.class));
        // a successful request should call the rest of the filter chain
        verify(mockFilterChain)
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }
    
    @Test
    public void testNoAuthRequired()
        throws IOException, ServletException, OAuthProblemException {
        RestrictToAuthNFilter testObj = new RestrictToAuthNFilter();
        when(mockRequest.getRequestURI()).thenReturn(NO_AUTH_REQUIRED_URI);
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // "public" URIs shouldn't result in a principal query
        verify(mockRequest, times(0)).getUserPrincipal();
        // a successful request should not send an error back to client
        verify(mockResponse, times(0)).sendError(any(Integer.class));
        // a successful request should call the rest of the filter chain
        verify(mockFilterChain)
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }

    @Test
    public void testInvalidRequestAuthRequired()
        throws IOException, ServletException, OAuthProblemException {
        RestrictToAuthNFilter testObj = new RestrictToAuthNFilter();
        when(mockRequest.getRequestURI()).thenReturn(AUTHN_REQUIRED_URI);
        testObj.doFilter(mockRequest, mockResponse, mockFilterChain);
        // an unauthenticated request should send an error back to client
        verify(mockResponse, times(1)).sendError(401);
        // an unauthenticated request should not call the rest of the filter chain
        verify(mockFilterChain, times(0))
            .doFilter(any(HttpServletRequestWrapper.class), eq(mockResponse));
    }
    
}
