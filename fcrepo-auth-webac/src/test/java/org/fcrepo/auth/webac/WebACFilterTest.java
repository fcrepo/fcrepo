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
package org.fcrepo.auth.webac;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author peichman
 */
@RunWith(MockitoJUnitRunner.class)
public class WebACFilterTest {

    private static final String testPath = "/testUri";

    private static final URI testURI = URI.create("http://localhost" + testPath);

    @Mock
    private SecurityManager mockSecurityManager;

    @InjectMocks
    private WebACFilter webacFilter = new WebACFilter();

    private static final WebACPermission readPermission = new WebACPermission(WEBAC_MODE_READ, testURI);

    private static final WebACPermission writePermission = new WebACPermission(WEBAC_MODE_WRITE, testURI);

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private MockFilterChain filterChain;

    private SubjectThreadState threadState;

    private Subject mockSubject;

    @Before
    public void setupRequest() {
        SecurityUtils.setSecurityManager(mockSecurityManager);

        mockSubject = Mockito.mock(Subject.class);
        threadState = new SubjectThreadState(mockSubject);
        threadState.bind();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    private void setupAdminUser() {
        // admin user
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(true);
    }

    private void setupAuthUserNoPerms() {
        // authenticated user without permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(false);
        when(mockSubject.isPermitted(writePermission)).thenReturn(false);
    }

    private void setupAuthUserReadOnly() {
        // authenticated user with only read permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(true);
        when(mockSubject.isPermitted(writePermission)).thenReturn(false);
    }

    private void setupAuthUserReadWrite() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(true);
        when(mockSubject.isPermitted(writePermission)).thenReturn(true);
    }

    @Test
    public void testAdminUserGet() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setRequestURI(testPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPost() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPut() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setRequestURI(testPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPatch() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserDelete() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsGet() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // GET => 403
        request.setRequestURI(testPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPost() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPut() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // PUT => 403
        request.setRequestURI(testPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPatch() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // PATCH => 403
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsDelete() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // DELETE => 403
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyGet() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // GET => 200
        request.setRequestURI(testPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPost() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPut() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // PUT => 403
        request.setRequestURI(testPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPatch() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // PATCH => 403
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyDelete() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // DELETE => 403
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteGet() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // GET => 200
        request.setRequestURI(testPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePost() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePut() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // PUT => 200
        request.setRequestURI(testPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePatch() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // PATCH => 200
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteDelete() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // DELETE => 200
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }
    @After
    public void clearSubject() {
        // unbind the subject to the thread
        threadState.restore();
    }
}
