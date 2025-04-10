/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_APPEND;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_CONTROL;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.EMBED_CONTAINED;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static java.util.stream.Stream.of;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDF;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author peichman
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WebACFilterTest {

    private static final String baseURL = "http://localhost";

    private static final String transactionId = "abc-def";

    private static final String transactionUri = baseURL + "/fcr:tx/" + transactionId;

    private static final String testPath = "/testUri";

    private static final String testChildPath = testPath + "/child";

    private static final String testAclPath = testPath + "/fcr:acl";

    private static final String testMembershipPath = "/testMembership";

    private static final URI testURI = URI.create(baseURL + testPath);

    private static final URI testAclURI = URI.create(baseURL + testAclPath);

    private static final URI testChildURI = URI.create(baseURL + testChildPath);

    private static final URI testMembershipURI = URI.create(baseURL + testMembershipPath);

    private static final FedoraId testId = FedoraId.create(testPath);

    private static final FedoraId testChildId = FedoraId.create(testChildPath);

    private static final FedoraId testMembershipId = FedoraId.create(testMembershipPath);

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

    private FedoraResource mockMembershipResource;

    @InjectMocks
    private final WebACFilter webacFilter = new WebACFilter();

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private MockFilterChain filterChain;

    private SubjectThreadState threadState;

    private Subject mockSubject;

    private FedoraPropsConfig propsConfig;

    @BeforeEach
    public void setupRequest() throws Exception {
        propsConfig = new FedoraPropsConfig();
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
        setField(webacFilter, "fedoraPropsConfig", propsConfig);

        mockContainer = Mockito.mock(Container.class);
        mockChildContainer = Mockito.mock(Container.class);
        mockBinary = Mockito.mock(Binary.class);
        mockRoot = Mockito.mock(Container.class);
        mockMembershipResource = Mockito.mock(Container.class);

        when(mockTransactionManager.get(transactionId)).thenReturn(mockTransaction);

        when(mockResourceFactory.getResource(mockTransaction, testChildId))
                .thenReturn(null);

        when(mockResourceFactory.getResource(mockTransaction, FedoraId.getRepositoryRootId()))
                .thenReturn(mockRoot);
        when(mockContainer.getContainer()).thenReturn(mockRoot);
        when(mockChildContainer.getContainer()).thenReturn(mockContainer);

        when(mockContainer.getTypes()).thenReturn(Arrays.asList(URI.create(BASIC_CONTAINER.toString())));
        when(mockContainer.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        when(mockChildContainer.getTypes()).thenReturn(Arrays.asList(URI.create(BASIC_CONTAINER.toString())));
        when(mockChildContainer.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        when(mockBinary.getTypes()).thenReturn(Arrays.asList(URI.create(NON_RDF_SOURCE.toString())));
        when(mockBinary.getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        when(mockMembershipResource.getTypes()).thenReturn(List.of(URI.create(BASIC_CONTAINER.getURI())));
        when(mockMembershipResource.getInteractionModel()).thenReturn(BASIC_CONTAINER.getURI());

        when(mockMembershipResource.getContainer()).thenReturn(mockRoot);

        final List<URI> rootTypes = new ArrayList<>();
        of("RepositoryRoot", "Resource", "Container").forEach(x -> rootTypes.add(URI.create(REPOSITORY_NAMESPACE +
                x)));
        when(mockRoot.getTypes()).thenReturn(rootTypes);
        when(mockRoot.getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());

        // Setup Container by default
        setupContainerResource();
    }

    @AfterEach
    public void clearSubject() {
        // unbind the subject to the thread
        threadState.restore();
    }

    /**
     * Make mockContainer returned for testId and mockChildContainer for testChildId
     */
    private void setupContainerResource() throws Exception {
        when(mockResourceFactory.getResource(mockTransaction, testId))
                .thenReturn(mockContainer);
        when(mockContainer.getFedoraId()). thenReturn(testId);
        when(mockResourceFactory.getResource(mockTransaction, testChildId))
                .thenReturn(mockChildContainer);
        when(mockChildContainer.getFedoraId()).thenReturn(testChildId);
    }

    /**
     * Make mockBinary returned for testId
     */
    private void setupBinaryResource() throws Exception {
        when(mockResourceFactory.getResource(mockTransaction, testId))
                .thenReturn(mockBinary);
        when(mockBinary.getFedoraId()).thenReturn(testId);
    }

    /**
     * Make mockContainer as direct container and mockChildContainer as the target of it's hasMemberRelation.
     */
    private void setupDirectResource() throws Exception {
        setupContainerResource();
        when(mockContainer.getTypes()).thenReturn(List.of(
                URI.create(FEDORA_CONTAINER.toString()),
                URI.create(DIRECT_CONTAINER.getURI()),
                URI.create(RDF_SOURCE.getURI())
        ));
        when(mockContainer.getInteractionModel()).thenReturn(DIRECT_CONTAINER.toString());
        when(mockResourceFactory.getResource(any(Transaction.class), eq(testMembershipId)))
                .thenReturn(mockMembershipResource);
        final var containerSubject = NodeFactory.createURI(testId.getFullId());
        final List<Triple> containerTriples = List.of(
            Triple.create(
                containerSubject,
                NodeFactory.createURI(MEMBERSHIP_RESOURCE.getURI()),
                NodeFactory.createURI(testMembershipURI.toString())
            ),
            Triple.create(
                containerSubject,
                RDF.type.asNode(),
                RDF_SOURCE.asNode()
            )
        );
        final var membershipSubject = NodeFactory.createURI(testMembershipId.getFullId());
        final List<Triple> targetTriples = List.of(
            Triple.create(
                membershipSubject,
                RDF.type.asNode(),
                NodeFactory.createURI(RDF_SOURCE.getURI())
            )
        );
        when(mockMembershipResource.getTriples())
                .thenReturn(new DefaultRdfStream(membershipSubject, targetTriples.stream()));
        when(mockContainer.getTriples()).thenReturn(new DefaultRdfStream(containerSubject, containerTriples.stream()));
        when(mockResourceFactory.getChildren(any(), eq(testId))).thenReturn(Stream.of(mockChildContainer));
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
        setUpPermissions(testURI, false, false, false, false);
    }

    private void setupAuthUserReadOnly() {
        // authenticated user with only read permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testURI, false, true, false, false);
    }

    private void setupAuthUserAppendOnly() {
        // authenticated user with only read permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testURI, false, false, true, false);
        setUpPermissions(testChildURI, false, false, true, false);
    }

    private void setupAuthUserReadAppend() {
        // authenticated user with only read permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testURI, false, true, true, false);
        setUpPermissions(testChildURI, false, false, true, false);
    }

    private void setupAuthUserReadWrite() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testURI, true, true, false, false);
    }

    private void setupAuthUserAclControl() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testAclURI, false, false, false, true);
    }

    private void setupAuthUserNoAclControl() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testAclURI, true, true, true, false);
    }

    private void setupAuthUserReadAppendWrite() {
        // authenticated user with read and write permissions
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testURI, true, true, true, true);
        setUpPermissions(testChildURI, false, false, true, false);
    }

    private void setupAuthUserReadParentAndChildren(final boolean accessToChild) {
        // authenticated user has read to a container and it's contained resources.
        when(mockSubject.isAuthenticated()).thenReturn(true);
        when(mockSubject.hasRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(mockSubject.hasRole(FEDORA_USER_ROLE)).thenReturn(true);
        setUpPermissions(testURI, false, true, false, false);
        // Contained resources are checked using internal URIs.
        setUpPermissions(URI.create(testChildId.getFullId()), false, accessToChild, false, false);
        when(mockResourceFactory.getChildren(any(), eq(testId))).thenReturn(Stream.of(mockChildContainer));
    }

    /**
     * Setup Shiro isPermitted results.
     * @param targetUri The target URI
     * @param writePerm Do you have WRITE permissions to the target URI?
     * @param readPerm Do you have READ permissions to the target URI?
     * @param appendPerm Do you have APPEND permissions to the target URI?
     * @param controlPerm Do you have CONTROL permissions to the target URI?
     */
    private void setUpPermissions(final URI targetUri, final boolean writePerm, final boolean readPerm,
                                  final boolean appendPerm, final boolean controlPerm) {
        when(mockSubject.isPermitted(new WebACPermission(WEBAC_MODE_READ, targetUri))).thenReturn(readPerm);
        when(mockSubject.isPermitted(new WebACPermission(WEBAC_MODE_APPEND, targetUri))).thenReturn(appendPerm);
        when(mockSubject.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, targetUri))).thenReturn(writePerm);
        when(mockSubject.isPermitted(new WebACPermission(WEBAC_MODE_CONTROL, targetUri))).thenReturn(controlPerm);
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

    @Test
    public void testAuthUserAppendDelete() throws Exception {
        setupAuthUserAppendOnly();
        // POST => 403
        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

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

    @Test
    public void testAuthUserReadWritePatch() throws Exception {
        setupAuthUserReadWrite();
        // PATCH => 200
        request.setMethod("PATCH");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    public void testAuthUserReadWriteDelete() throws Exception {
        setupAuthUserReadWrite();
        // DELETE => 200
        request.setMethod("DELETE");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

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

    /**
     * Test to verify a user with read/write access POSTing to a direct container
     */
    @Test
    public void testDoPostDirectContainerOk() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();
        request.setRequestURI(testPath);
        request.setMethod("POST");
        request.setContentType("application/n-triples");
        // membership resource (mockMembership) also need read/write permission
        setUpPermissions(testMembershipURI, true, true, true, true);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    /**
     * Test a user with read only permissions failing to POST to a direct container
     */
    @Test
    public void testDoPostDirectContainerFailed() throws Exception {
        setupAuthUserReadOnly();
        setupDirectResource();
        request.setRequestURI(testPath);
        request.setMethod("POST");
        request.setContentType("application/n-triples");
        // membership resource (mockMembership) also need read permission
        setUpPermissions(testMembershipURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test when a user has permission to the testPath but not to the membership resource
     */
    @Test
    public void testDoPostDirectContainerMembershipFail() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();
        request.setRequestURI(testPath);
        request.setMethod("POST");
        request.setContentType("application/n-triples");
        // membership resource (mockMembership) only gets read permission
        setUpPermissions(testMembershipURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test to verify a user with read/write access PUTing to a direct container.
     * When PUTting we check if the parent is a direct container.
     */
    @Test
    public void testDoPutDirectContainerOk() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();

        request.setRequestURI(testChildPath);
        request.setMethod("PUT");
        request.setContentType("application/n-triples");
        // membership resource (mockMembership) also need read/write permission
        setUpPermissions(testMembershipURI, true, true, true, true);
        setUpPermissions(testChildURI, true, true, true, true);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    /**
     * Test a user with read only permissions failing to PUT to a direct container
     */
    @Test
    public void testDoPutDirectContainerFailed() throws Exception {
        setupAuthUserReadOnly();
        setupDirectResource();
        request.setRequestURI(testChildPath);
        request.setMethod("PUT");
        request.setContentType("application/n-triples");
        // membership resource (mockMembership) also need read/write permission
        setUpPermissions(testMembershipURI, false, true, false, false);
        setUpPermissions(testChildURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test when a user has permission to the testPath but not to the membership resource
     */
    @Test
    public void testDoPutDirectContainerMembershipFail() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();
        request.setRequestURI(testChildPath);
        request.setMethod("PUT");
        request.setContentType("application/n-triples");
        // membership resource (mockMembership) only gets read permission
        setUpPermissions(testMembershipURI, false, true, false, false);
        // child resource (mockChild) only gets read/write permissions
        setUpPermissions(testChildURI, true, true, true, true);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test to verify a user with read/write access PATCHing to a direct container
     */
    @Test
    public void testDoPatchDirectContainerOk() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();

        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        request.setContentType("application/sparql-update");
        final String content = "INSERT { <> <" + MEMBERSHIP_RESOURCE + "> <" + testMembershipURI + "> } WHERE { }";
        request.setContent(content.getBytes(StandardCharsets.UTF_8));
        // Give full writes to the membership resource
        setUpPermissions(testMembershipURI, true, true, true, true);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    /**
     * Test a user with read only permissions failing to PATCH to a direct container
     */
    @Test
    public void testDoPatchDirectContainerFailed() throws Exception {
        setupAuthUserReadOnly();
        setupDirectResource();

        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        request.setContentType("application/sparql-update");
        final String content = "INSERT { <> <" + MEMBERSHIP_RESOURCE + "> <" + testMembershipURI + "> } WHERE { }";
        request.setContent(content.getBytes(StandardCharsets.UTF_8));
        // Give read only on the membership resource
        setUpPermissions(testMembershipURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test a user with read/write permissions failing to PATCH to a direct container, because we don't have
     * write permissions on the membership resource.
     */
    @Test
    public void testDoPatchDirectContainerMembershipFailed() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();

        request.setRequestURI(testPath);
        request.setMethod("PATCH");
        request.setContentType("application/sparql-update");
        final String content = "INSERT { <> <" + MEMBERSHIP_RESOURCE + "> <" + testMembershipURI + "> } WHERE { }";
        request.setContent(content.getBytes(StandardCharsets.UTF_8));
        // Give read only on the membership resource
        setUpPermissions(testMembershipURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test to verify a user with read/write access DELETEing to a direct container
     */
    @Test
    public void testDoDeleteDirectContainerOk() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();

        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        // Give full writes to the membership resource
        setUpPermissions(testMembershipURI, true, true, true, true);
        // Need access to contained resources to delete them, contained resources are checked using internal URIs.
        setUpPermissions(URI.create(testChildId.getFullId()), true, true, true, true);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    /**
     * Test a user with read only permissions failing to DELETE to a direct container
     */
    @Test
    public void testDoDeleteDirectContainerFailed() throws Exception {
        setupAuthUserReadOnly();
        setupDirectResource();

        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        // Give read only on the membership resource
        setUpPermissions(testMembershipURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test a user with read/write permissions failing to DELETE to a direct container, because we don't have
     * write permissions on the membership resource.
     */
    @Test
    public void testDoDeleteDirectContainerMembershipFailed() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();

        request.setRequestURI(testPath);
        request.setMethod("DELETE");
        // Give read only on the membership resource
        setUpPermissions(testMembershipURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test to verify a user with read/write access DELETEing a direct container member
     */
    @Test
    public void testDoDeleteDirectContainerMemberOk() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();

        request.setRequestURI(testChildPath);
        request.setMethod("DELETE");
        // Give full writes to the membership resource
        setUpPermissions(testMembershipURI, true, true, true, true);
        // Need access to contained resources to delete them, contained resources are checked using internal URIs.
        setUpPermissions(testChildURI, true, true, true, true);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    /**
     * Test to verify a user with read/write access DELETEing a direct container member
     */
    @Test
    public void testDoDeleteDirectContainerMemberFailed() throws Exception {
        setupAuthUserReadWrite();
        setupDirectResource();

        request.setRequestURI(testChildPath);
        request.setMethod("DELETE");
        // Give full writes to the membership resource
        setUpPermissions(testMembershipURI, true, true, true, true);
        // Need access to contained resources to delete them, contained resources are checked using internal URIs.
        setUpPermissions(testChildURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test creating a direct container with permissions to the membership resource
     */
    @Test
    public void testCreateDirectContainerOk() throws Exception {
        setupAuthUserReadWrite();
        setupContainerResource();

        request.setRequestURI(testPath);
        request.setMethod("POST");
        request.setContentType("text/turtle");
        request.addHeader("Link", "<" + DIRECT_CONTAINER.getURI() + ">; rel=\"type\"");
        final var content = "<> a <" + DIRECT_CONTAINER.getURI() + "> ; " +
                "<" + MEMBERSHIP_RESOURCE.getURI() + "> <" + testMembershipURI + "> .";
        request.setContent(content.getBytes(StandardCharsets.UTF_8));
        setUpPermissions(testMembershipURI, true, true, true, true);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_OK, response.getStatus());
    }

    /**
     * Test creating a direct container without permissions to the membership resource
     */
    @Test
    public void testCreateDirectContainerFailed() throws Exception {
        setupAuthUserReadWrite();
        setupContainerResource();

        request.setRequestURI(testPath);
        request.setMethod("POST");
        request.setContentType("text/turtle");
        request.addHeader("Link", "<" + DIRECT_CONTAINER.getURI() + ">; rel=\"type\"");
        final var content = "<> a <" + DIRECT_CONTAINER.getURI() + "> ; " +
                "<" + MEMBERSHIP_RESOURCE.getURI() + "> <" + testMembershipURI + "> .";
        request.setContent(content.getBytes(StandardCharsets.UTF_8));
        setUpPermissions(testMembershipURI, false, true, false, false);
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    /**
     * Test a request with an invalid Memento URI.
     */
    @Test
    public void testInvalidMementoRequest() throws Exception {
        request.setRequestURI(testPath + "/" + FCR_VERSIONS + "/1234567890");
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_BAD_REQUEST, response.getStatus());
    }

    /**
     * Test a request with an invalid resource identifier.
     */
    @Test
    public void testInvalidResourceIdentifier() throws Exception {
        request.setRequestURI(testPath + "/" + FCR_VERSIONS + "/1/" + FCR_VERSIONS + "/123456");
        request.setMethod("GET");
        webacFilter.doFilter(request, response, filterChain);
        assertEquals(SC_BAD_REQUEST, response.getStatus());
    }
}
