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

import static java.util.stream.Stream.of;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_APPEND;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_CONTROL;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

    private static final String transactionId = "abc-def";

    private static final String transactionUri = baseURL + "/fcr:tx/" + transactionId;

    private static final String testPath = "/testUri";

    private static final String testChildPath = testPath + "/child";

    private static final String testAclPath = testPath + "/fcr:acl";

    private static final URI testURI = URI.create(baseURL + testPath);

    private static final URI testAclURI = URI.create(baseURL + testAclPath);

    private static final URI testChildURI = URI.create(baseURL + testChildPath);

    private static final FedoraId testId = FedoraId.create(testPath);

    private static final FedoraId testChildId = FedoraId.create(testChildPath);

    @Mock
    private SecurityManager mockSecurityManager;

    @Mock
    private TransactionManager mockTransactionManager;

    @Mock
    private ResourceFactory mockResourceFactory;

    @Mock
    private Transaction mockTransaction;

    private FedoraResource mockContainer;

    private FedoraResource mockChildContainer;

    private FedoraResource mockBinary;

    private FedoraResource mockRoot;

    @InjectMocks
    private final WebACFilter webacFilter = new WebACFilter();

    private static final WebACPermission readPermission = new WebACPermission(WEBAC_MODE_READ, testURI);

    private static final WebACPermission appendPermission = new WebACPermission(WEBAC_MODE_APPEND, testURI);

    private static final WebACPermission writePermission = new WebACPermission(WEBAC_MODE_WRITE, testURI);

    private static final WebACPermission controlPermission = new WebACPermission(WEBAC_MODE_CONTROL, testURI);

    private static final WebACPermission readAclPermission = new WebACPermission(WEBAC_MODE_READ, testAclURI);
    private static final WebACPermission appendAclPermission = new WebACPermission(WEBAC_MODE_APPEND, testAclURI);
    private static final WebACPermission writeAclPermission = new WebACPermission(WEBAC_MODE_WRITE, testAclURI);
    private static final WebACPermission controlAclPermission = new WebACPermission(WEBAC_MODE_CONTROL, testAclURI);

    // We are dealing with internal IDs.
    private static final WebACPermission readChildPermission = new WebACPermission(WEBAC_MODE_READ,
            URI.create(testChildId.getFullId()));
    private static final WebACPermission appendChildPermission = new WebACPermission(WEBAC_MODE_APPEND, testChildURI);
    private static final WebACPermission writeChildPermission = new WebACPermission(WEBAC_MODE_WRITE, testChildURI);
    private static final WebACPermission controlChildPermission = new WebACPermission(WEBAC_MODE_CONTROL, testChildURI);

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private MockFilterChain filterChain;

    private SubjectThreadState threadState;

    private Subject mockSubject;

    @Before
    public void setupRequest() throws Exception {
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
        request.setContentType(null);
        request.addHeader(ATOMIC_ID_HEADER, transactionUri);

        setField(webacFilter, "transactionManager", mockTransactionManager);

        mockContainer = Mockito.mock(Container.class);
        mockChildContainer = Mockito.mock(Container.class);
        mockBinary = Mockito.mock(Binary.class);
        mockRoot = Mockito.mock(Container.class);

        when(mockTransactionManager.get(transactionId)).thenReturn(mockTransaction);

        when(mockResourceFactory.getResource(mockTransaction, testChildId))
                .thenReturn(null);

        when(mockResourceFactory.getResource(mockTransaction, FedoraId.getRepositoryRootId()))
                .thenReturn(mockRoot);
        when(mockContainer.getContainer()).thenReturn(mockRoot);
        when(mockChildContainer.getContainer()).thenReturn(mockContainer);

        when(mockContainer.getTypes()).thenReturn(Arrays.asList(URI.create(BASIC_CONTAINER.toString())));
        when(mockChildContainer.getTypes()).thenReturn(Arrays.asList(URI.create(BASIC_CONTAINER.toString())));
        when(mockBinary.getTypes()).thenReturn(Arrays.asList(URI.create(NON_RDF_SOURCE.toString())));

        final List<URI> rootTypes = new ArrayList<>();
        of("RepositoryRoot", "Resource", "Container").forEach(x -> rootTypes.add(URI.create(REPOSITORY_NAMESPACE +
                x)));
        when(mockRoot.getTypes()).thenReturn(rootTypes);

        // Setup Container by default
        setupContainerResource();
    }

    private void setupContainerResource() throws Exception {
        when(mockResourceFactory.getResource(mockTransaction, testId))
                .thenReturn(mockContainer);
        when(mockContainer.getFedoraId()). thenReturn(testId);
        when(mockResourceFactory.getResource(mockTransaction, testChildId))
                .thenReturn(mockChildContainer);
        when(mockChildContainer.getFedoraId()).thenReturn(testChildId);
    }

    private void setupBinaryResource() throws Exception {
        when(mockResourceFactory.getResource(mockTransaction, testId))
                .thenReturn(mockBinary);
        when(mockBinary.getFedoraId()).thenReturn(testId);
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

    private void setupAuthUserReadParentAndChildren(final boolean accessToChild) {
        // authenticated user has read to a container and it's contained resources.
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        when(mockSubject.isPermitted(readPermission)).thenReturn(true);
        when(mockSubject.isPermitted(appendPermission)).thenReturn(false);
        when(mockSubject.isPermitted(writePermission)).thenReturn(false);
        when(mockSubject.isPermitted(controlPermission)).thenReturn(false);
        when(mockSubject.isPermitted(readChildPermission)).thenReturn(accessToChild);
        when(mockSubject.isPermitted(appendChildPermission)).thenReturn(false);
        when(mockSubject.isPermitted(writeChildPermission)).thenReturn(false);
        when(mockSubject.isPermitted(controlChildPermission)).thenReturn(false);
        when(mockResourceFactory.getChildren(any(), eq(testId))).thenReturn(List.of(mockChildContainer).stream());
    }

    private void setupEmbeddedResourceHeader() {
        request.addHeader("Prefer", "return=representation; include=\"" + EMBED_CONTAINED + "\"");
    }

    @Test
    public void testAdminUserHead() throws Exception {
        setupAdminUser();
        // HEAD => 200
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserOptions() throws Exception {
        setupAdminUser();
        // GET => 200
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserGet() throws Exception {
        setupAdminUser();
        // GET => 200
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPost() throws Exception {
        setupAdminUser();
        // GET => 200
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPut() throws Exception {
        setupAdminUser();
        // GET => 200
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserPatch() throws Exception {
        setupAdminUser();
        // GET => 200
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAdminUserDelete() throws Exception {
        setupAdminUser();
        // GET => 200
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsHead() throws Exception {
        setupAuthUserNoPerms();
        // HEAD => 403
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsOptions() throws Exception {
        setupAuthUserNoPerms();
        // GET => 403
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsGet() throws Exception {
        setupAuthUserNoPerms();
        // GET => 403
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPost() throws Exception {
        setupAuthUserNoPerms();
        // POST => 403
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPut() throws Exception {
        setupAuthUserNoPerms();
        // PUT => 403
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsPatch() throws Exception {
        setupAuthUserNoPerms();
        // PATCH => 403
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserNoPermsDelete() throws Exception {
        setupAuthUserNoPerms();
        // DELETE => 403
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyHead() throws Exception {
        setupAuthUserReadOnly();
        // HEAD => 200
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyOptions() throws Exception {
        setupAuthUserReadOnly();
        // GET => 200
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyGet() throws Exception {
        setupAuthUserReadOnly();
        // GET => 200
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPost() throws Exception {
        setupAuthUserReadOnly();
        // POST => 403
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPut() throws Exception {
        setupAuthUserReadOnly();
        // PUT => 403
        request.setMethod("PUT");
        request.setRequestURI(testPath);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyPatch() throws Exception {
        setupAuthUserReadOnly();
        // PATCH => 403
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadOnlyDelete() throws Exception {
        setupAuthUserReadOnly();
        // DELETE => 403
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPatchNonSparqlContent() throws Exception {
        setupAuthUserReadAppend();
        // PATCH (Non Sparql Content) => 403
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPatchSparqlNoContent() throws Exception {
        setupAuthUserReadAppend();
        // PATCH (Sparql No Content) => 200 (204)
        request.setContentType(contentTypeSPARQLUpdate);
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadAppendPatchSparqlInvalidContent() throws Exception {
        setupAuthUserReadAppend();
        // PATCH (Sparql Invalid Content) => 403
        request.setContentType(contentTypeSPARQLUpdate);
        request.setContent("SOME TEXT".getBytes());
        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadAppendPatchSparqlInsert() throws Exception {
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

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadAppendPatchSparqlDelete() throws Exception {
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

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserAppendPostContainer() throws Exception {
        setupAuthUserAppendOnly();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserAppendPostBinary() throws Exception {
        setupAuthUserAppendOnly();
        setupBinaryResource();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserAppendDelete() throws Exception {
        setupAuthUserAppendOnly();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadAppendPostContainer() throws Exception {
        setupAuthUserReadAppend();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendPostBinary() throws Exception {
        setupAuthUserReadAppend();
        setupBinaryResource();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadAppendDelete() throws Exception {
        setupAuthUserReadAppend();
        // DELETE => 403
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendWritePostContainer() throws Exception {
        setupAuthUserReadAppendWrite();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadAppendWritePostBinary() throws Exception {
        setupAuthUserReadAppendWrite();
        setupBinaryResource();
        // POST => 200
        request.setRequestURI(testPath);
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteHead() throws Exception {
        setupAuthUserReadWrite();
        // HEAD => 200
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteOptions() throws Exception {
        setupAuthUserReadWrite();
        // GET => 200
        request.setMethod("OPTIONS");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteGet() throws Exception {
        setupAuthUserReadWrite();
        // GET => 200
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePost() throws Exception {
        setupAuthUserReadWrite();
        // POST => 200
        request.setMethod("POST");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWritePut() throws Exception {
        setupAuthUserReadWrite();
        // PUT => 200
        request.setMethod("PUT");
        request.setRequestURI(testPath);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadWritePatch() throws Exception {
        setupAuthUserReadWrite();
        // PATCH => 200
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadWriteDelete() throws Exception {
        setupAuthUserReadWrite();
        // DELETE => 200
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Ignore // TODO FIX THIS TEST
    @Test
    public void testAuthUserReadAppendWriteDelete() throws Exception {
        setupAuthUserReadAppendWrite();
        // DELETE => 200
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserAppendPutNewChild() throws Exception {
        setupAuthUserAppendOnly();
        // PUT => 200
        when(mockResourceFactory.getResource((Transaction)any(), eq(testChildId)))
                .thenThrow(PathNotFoundException.class);
        request.setRequestURI(testChildPath);
        request.setPathInfo(testChildPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAclControlPutToAcl() throws Exception {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlPutToAcl() throws Exception {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PUT");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlGetToAcl() throws Exception {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlGetToAcl() throws Exception {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlHeadToAcl() throws Exception {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlHeadToAcl() throws Exception {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("HEAD");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlPatchToAcl() throws Exception {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlPatchToAcl() throws Exception {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclControlDelete() throws Exception {
        setupAuthUserAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testNoAclControlDelete() throws Exception {
        setupAuthUserNoAclControl();
        request.setRequestURI(testAclPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void testAclReadEmbeddedOk() throws Exception {
        setupAuthUserReadParentAndChildren(true);
        setupEmbeddedResourceHeader();
        request.setRequestURI(testPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAclReadEmbeddedDenied() throws Exception {
        setupAuthUserReadParentAndChildren(false);
        setupEmbeddedResourceHeader();
        request.setRequestURI(testPath);
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @After
    public void clearSubject() {
        // unbind the subject to the thread
        threadState.restore();
    }
}
