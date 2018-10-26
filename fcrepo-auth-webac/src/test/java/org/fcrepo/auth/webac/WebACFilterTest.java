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
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_APPEND;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_CONTROL;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import javax.servlet.ServletException;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.ContainerImpl;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author peichman
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class WebACFilterTest {

    private static final String baseURL = "http://localhost";

    private static final String testPath = "/testUri";

    private static final String testChildPath = testPath + "/child";

    private static final String testAclPath = testPath + "/fcr:acl";

    private static final URI testURI = URI.create(baseURL + testPath);

    private static final URI testAclURI = URI.create(baseURL + testAclPath);

    private static final URI testChildURI = URI.create(baseURL + testChildPath);

    @Mock
    private SecurityManager mockSecurityManager;

    @Mock
    private SessionFactory mockSessionFactory;

    @Mock
    private NodeService mockNodeService;

    private FedoraSession mockFedoraSession;

    private FedoraResource mockContainer;

    private FedoraResource mockBinary;

    @InjectMocks
    private final WebACFilter webacFilter = new WebACFilter();

    private static final WebACPermission readPermission = new WebACPermission(WEBAC_MODE_READ, testURI);

    private static final WebACPermission appendPermission = new WebACPermission(WEBAC_MODE_APPEND, testURI);

    private static final WebACPermission appendChildPermission = new WebACPermission(WEBAC_MODE_APPEND, testChildURI);

    private static final WebACPermission writePermission = new WebACPermission(WEBAC_MODE_WRITE, testURI);

    private static final WebACPermission controlPermission = new WebACPermission(WEBAC_MODE_CONTROL, testURI);

    private static final WebACPermission readAclPermission = new WebACPermission(WEBAC_MODE_READ, testAclURI);
    private static final WebACPermission appendAclPermission = new WebACPermission(WEBAC_MODE_APPEND, testAclURI);
    private static final WebACPermission writeAclPermission = new WebACPermission(WEBAC_MODE_WRITE, testAclURI);
    private static final WebACPermission controlAclPermission = new WebACPermission(WEBAC_MODE_CONTROL, testAclURI);

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

        // set default request URI and path info
        // for the purposes of this test, there is no context path
        // so the request URI and path info are the same
        request.setPathInfo(testPath);
        request.setRequestURI(testPath);

        mockContainer = Mockito.mock(ContainerImpl.class);
        mockBinary = Mockito.mock(FedoraBinaryImpl.class);

        when(mockSessionFactory.getInternalSession()).thenReturn(mockFedoraSession);

        when(mockNodeService.exists(mockFedoraSession, testPath)).thenReturn(true);
        when(mockNodeService.exists(mockFedoraSession, testChildPath)).thenReturn(false);

        when(mockContainer.getTypes()).thenReturn(Arrays.asList(URI.create(BASIC_CONTAINER.toString())));
        when(mockBinary.getTypes()).thenReturn(Arrays.asList(URI.create(NON_RDF_SOURCE.toString())));
    }

    private void setupContainerResource() {
        when(mockNodeService.find(mockFedoraSession, testPath)).thenReturn(mockContainer);
        when(mockBinary.hasType(FEDORA_BINARY)).thenReturn(false);
    }

    private void setupBinaryResource() {
        when(mockNodeService.find(mockFedoraSession, testPath)).thenReturn(mockBinary);
        when(mockBinary.hasType(FEDORA_BINARY)).thenReturn(true);
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
        when(mockSubject.isPermitted(appendPermission)).thenReturn(false);
        when(mockSubject.isPermitted(writePermission)).thenReturn(false);
        when(mockSubject.isPermitted(controlPermission)).thenReturn(false);

    }

    private void setupAuthUserReadOnly() {
        // authenticated user with only read permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendPermission)).thenReturn(false);
        when(mockSubject.isPermitted(writePermission)).thenReturn(false);
        when(mockSubject.isPermitted(controlPermission)).thenReturn(false);

    }

    private void setupAuthUserAppendOnly() {
        // authenticated user with only read permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(false);
        when(mockSubject.isPermitted(appendPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendChildPermission)).thenReturn(true);
        when(mockSubject.isPermitted(writePermission)).thenReturn(false);
        when(mockSubject.isPermitted(controlPermission)).thenReturn(false);

    }

    private void setupAuthUserReadAppend() {
        // authenticated user with only read permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendChildPermission)).thenReturn(true);
        when(mockSubject.isPermitted(writePermission)).thenReturn(false);
        when(mockSubject.isPermitted(controlPermission)).thenReturn(false);
    }

    private void setupAuthUserReadWrite() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendPermission)).thenReturn(false);
        when(mockSubject.isPermitted(writePermission)).thenReturn(true);
        when(mockSubject.isPermitted(controlPermission)).thenReturn(false);
    }

    private void setupAuthUserAclControl() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readAclPermission)).thenReturn(false);
        when(mockSubject.isPermitted(appendAclPermission)).thenReturn(false);
        when(mockSubject.isPermitted(writeAclPermission)).thenReturn(false);
        when(mockSubject.isPermitted(controlAclPermission)).thenReturn(true);
    }

    private void setupAuthUserNoAclControl() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readAclPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendAclPermission)).thenReturn(true);
        when(mockSubject.isPermitted(writeAclPermission)).thenReturn(true);
        when(mockSubject.isPermitted(controlAclPermission)).thenReturn(false);
    }

    private void setupAuthUserReadAppendWrite() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendChildPermission)).thenReturn(true);
        when(mockSubject.isPermitted(writePermission)).thenReturn(true);
        when(mockSubject.isPermitted(controlPermission)).thenReturn(true);

    }

    @Test
    public void testAdminUserHead() throws ServletException, IOException {
        setupAdminUser();
        // HEAD => 200
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserOptions() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserGet() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPost() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPut() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPatch() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserDelete() throws ServletException, IOException {
        setupAdminUser();
        // GET => 200
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsHead() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // HEAD => 403
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsOptions() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // GET => 403
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsGet() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // GET => 403
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPost() throws ServletException, IOException {
        setupAuthUserNoPerms();
        setupContainerResource();
        // POST => 403
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPut() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // PUT => 403
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPatch() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // PATCH => 403
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsDelete() throws ServletException, IOException {
        setupAuthUserNoPerms();
        // DELETE => 403
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyHead() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // HEAD => 200
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyOptions() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // GET => 200
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyGet() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // GET => 200
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPost() throws ServletException, IOException {
        setupAuthUserReadOnly();
        setupContainerResource();
        // POST => 403
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPut() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // PUT => 403
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPatch() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // PATCH => 403
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyDelete() throws ServletException, IOException {
        setupAuthUserReadOnly();
        // DELETE => 403
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPatchNonSparqlContent() throws ServletException, IOException {
        setupAuthUserReadAppend();
        // PATCH (Non Sparql Content) => 403
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPatchSparqlNoContent() throws ServletException, IOException {
        setupAuthUserReadAppend();
        // PATCH (Sparql No Content) => 200 (204)
        request.setContentType(contentTypeSPARQLUpdate);
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPatchSparqlInvalidContent() throws ServletException, IOException {
        setupAuthUserReadAppend();
        // PATCH (Sparql Invalid Content) => 403
        request.setContentType(contentTypeSPARQLUpdate);
        request.setContent("SOME TEXT".getBytes());
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPatchSparqlInsert() throws ServletException, IOException {
        setupAuthUserReadAppend();
        // PATCH (Sparql INSERT) => 200 (204)
        final String updateString =
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"new title\" } WHERE { }";
        request.setContentType(contentTypeSPARQLUpdate);
        request.setContent(updateString.getBytes());
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPatchSparqlDelete() throws ServletException, IOException {
        setupAuthUserReadAppend();
        // PATCH (Sparql DELETE) => 403
        final String updateString =
                "DELETE { <> <http://purl.org/dc/elements/1.1/title> \"new title\" } WHERE { }";
        request.setContentType(contentTypeSPARQLUpdate);
        request.setContent(updateString.getBytes());
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserAppendPostContainer() throws IOException, ServletException {
        setupAuthUserAppendOnly();
        setupContainerResource();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserAppendPostBinary() throws IOException, ServletException {
        setupAuthUserAppendOnly();
        setupBinaryResource();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserAppendDelete() throws IOException, ServletException {
        setupAuthUserAppendOnly();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPostContainer() throws IOException, ServletException {
        setupAuthUserReadAppend();
        setupContainerResource();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPostBinary() throws IOException, ServletException {
        setupAuthUserReadAppend();
        setupBinaryResource();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendDelete() throws ServletException, IOException {
        setupAuthUserReadAppend();
        // DELETE => 403
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendWritePostContainer() throws IOException, ServletException {
        setupAuthUserReadAppendWrite();
        setupContainerResource();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendWritePostBinary() throws IOException, ServletException {
        setupAuthUserReadAppendWrite();
        setupBinaryResource();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteHead() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // HEAD => 200
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteOptions() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // GET => 200
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteGet() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // GET => 200
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePost() throws ServletException, IOException {
        setupAuthUserReadWrite();
        setupContainerResource();
        // POST => 200
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePut() throws ServletException, IOException {
        setupAuthUserReadWrite();
        setupContainerResource();
        // PUT => 200
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePatch() throws ServletException, IOException {
        setupAuthUserReadWrite();
        setupContainerResource();
        // PATCH => 200
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteDelete() throws ServletException, IOException {
        setupAuthUserReadWrite();
        // DELETE => 200
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendWriteDelete() throws ServletException, IOException {
        setupAuthUserReadAppendWrite();
        // DELETE => 200
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserAppendPutNewChild() throws IOException, ServletException {
        setupAuthUserAppendOnly();
        // PUT => 200
        request.setRequestURI(testChildPath);
        request.setPathInfo(testChildPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAclControlPutToAcl() throws ServletException, IOException {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlPutToAcl() throws ServletException, IOException {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlGetToAcl() throws ServletException, IOException {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlGetToAcl() throws ServletException, IOException {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlHeadToAcl() throws ServletException, IOException {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlHeadToAcl() throws ServletException, IOException {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlPatchToAcl() throws ServletException, IOException {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlPatchToAcl() throws ServletException, IOException {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlDelete() throws ServletException, IOException {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlDelete() throws ServletException, IOException {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @After
    public void clearSubject() {
        // unbind the subject to the thread
        threadState.restore();
    }
}
