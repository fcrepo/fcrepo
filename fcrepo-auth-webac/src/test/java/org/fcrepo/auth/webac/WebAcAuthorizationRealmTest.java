/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE_VALUE;
import static org.fcrepo.auth.webac.WebACAuthorizingRealm.URIS_TO_AUTHORIZE;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.web.subject.WebSubject;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider;
import org.fcrepo.auth.common.DelegateHeaderPrincipalProvider;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryConfigurationException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.util.ThreadContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link WebACAuthorizingRealm}
 *
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
public class WebAcAuthorizationRealmTest {

    private URI requestUri = URI.create("http://localhost:8080/fcrepo/rest/test");

    @Mock
    private FedoraPropsConfig fedoraPropsConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private WebACRolesProvider rolesProvider;

    @Mock
    private TransactionManager transactionManager;

    @Mock
    private ResourceFactory resourceFactory;

    @Mock
    private ContainmentIndex containmentIndex;

    @Mock
    private FedoraResource resource;

    @InjectMocks
    private WebACAuthorizingRealm webACAuthorizingRealm;

    private DefaultWebSecurityManager securityManager;

    private Principal basicUserPrincipal;

    private Principal containerRolesPrincipal;

    private SimplePrincipalCollection principalCollection;

    private void doPrincipalStubbings() throws PathNotFoundException {
        basicUserPrincipal = new BasicUserPrincipal("testUser");
        containerRolesPrincipal = new ContainerRolesPrincipalProvider.ContainerRolesPrincipal("fedoraUser");
        principalCollection = new SimplePrincipalCollection();
        principalCollection.add(basicUserPrincipal, "testRealm");
        principalCollection.add(containerRolesPrincipal, "testRealm");
    }

    private void doRequestStubbings() throws PathNotFoundException {
        final var urisToAuthorize = Set.of(
                requestUri
        );
        when(request.getAttribute(URIS_TO_AUTHORIZE)).thenReturn(urisToAuthorize);
        when(request.getScheme()).thenReturn(requestUri.getScheme());
        when(request.getServerName()).thenReturn(requestUri.getHost());
        when(request.getServerPort()).thenReturn(requestUri.getPort());
        final var path = requestUri.getPath().split("/");
        when(request.getContextPath()).thenReturn("/" + path[1]);
        when(request.getServletPath()).thenReturn("/" + path[2]);
        when(request.getRequestURL()).thenReturn(new StringBuffer(requestUri.toString()));
        when(resourceFactory.getResource(any(Transaction.class), eq(FedoraId.create("test")))).thenReturn(resource);

    }

    private void doAllStubbings() throws PathNotFoundException {
        doPrincipalStubbings();
        doRequestStubbings();
    }

    @BeforeEach
    public void setUpShiro() {
        securityManager = new DefaultWebSecurityManager();
        SecurityUtils.setSecurityManager(securityManager);
        ThreadContext.bind(securityManager);
        final var subject = new WebSubject.Builder(securityManager, request, response).buildWebSubject();
        ThreadContext.bind(subject);
    }

    @Test
    public void testNoUri() throws PathNotFoundException {
        doPrincipalStubbings();
        when(request.getAttribute(URIS_TO_AUTHORIZE)).thenReturn(null);
        final AuthorizationInfo authzinfo = webACAuthorizingRealm.doGetAuthorizationInfo(principalCollection);
        assertEquals(1, authzinfo.getRoles().size());
        assertNull(authzinfo.getStringPermissions());
        assertNull(authzinfo.getObjectPermissions());
    }

    @Test
    public void testDoGetAuthorizationInfoUser() throws PathNotFoundException {
        doAllStubbings();
        final Map<String, Collection<String>> roles = new HashMap<>();
        roles.put("testUser", Set.of(WEBAC_MODE_READ_VALUE, WEBAC_MODE_WRITE_VALUE));
        when(rolesProvider.getRoles(eq(resource), any(Transaction.class))).thenReturn(roles);
        final AuthorizationInfo authzinfo = webACAuthorizingRealm.doGetAuthorizationInfo(principalCollection);
        assertEquals(1, authzinfo.getRoles().size());
        assertTrue(authzinfo.getRoles().contains("fedoraUser"));
        assertNull(authzinfo.getStringPermissions());
        assertEquals(2, authzinfo.getObjectPermissions().size());
        assertTrue(authzinfo.getObjectPermissions().stream()
                .anyMatch(p -> p.implies(new WebACPermission(WEBAC_MODE_READ, requestUri))));
        assertTrue(authzinfo.getObjectPermissions().stream()
                .anyMatch(p -> p.implies(new WebACPermission(WEBAC_MODE_WRITE, requestUri))));
    }

    @Test
    public void testDoGetAuthorizationInfoAdmin() {
        principalCollection = new SimplePrincipalCollection();
        principalCollection.add(new BasicUserPrincipal("admin"), "testRealm");
        principalCollection.add(new ContainerRolesPrincipalProvider.ContainerRolesPrincipal("fedoraAdmin"),
                "testRealm");
        final AuthorizationInfo authzinfo = webACAuthorizingRealm.doGetAuthorizationInfo(principalCollection);
        assertEquals(1, authzinfo.getRoles().size());
        assertTrue(authzinfo.getRoles().contains("fedoraAdmin"));
        assertNull(authzinfo.getStringPermissions());
        assertNull(authzinfo.getObjectPermissions());
    }

    @Test
    public void testMultipleDelegateHeaders() {
        principalCollection = new SimplePrincipalCollection();
        principalCollection.add(new BasicUserPrincipal("admin"), "testRealm");
        principalCollection.add(new ContainerRolesPrincipalProvider.ContainerRolesPrincipal("fedoraAdmin"),
                "testRealm");
        principalCollection.add(new DelegateHeaderPrincipalProvider().createPrincipal("DELEGATE1"), "testRealm");
        principalCollection.add(new DelegateHeaderPrincipalProvider().createPrincipal("DELEGATE2"), "testRealm");
        assertThrows(RepositoryConfigurationException.class, () -> {
            webACAuthorizingRealm.doGetAuthorizationInfo(principalCollection);
        });
    }

    @Test
    public void testSingleDelegateHeaders() throws PathNotFoundException {
        doRequestStubbings();
        principalCollection = new SimplePrincipalCollection();
        principalCollection.add(new BasicUserPrincipal("admin"), "testRealm");
        principalCollection.add(new ContainerRolesPrincipalProvider.ContainerRolesPrincipal("fedoraAdmin"),
                "testRealm");
        principalCollection.add(new DelegateHeaderPrincipalProvider().createPrincipal("testUser"), "testRealm");
        final Map<String, Collection<String>> roles = new HashMap<>();
        roles.put("testUser", Set.of(WEBAC_MODE_READ_VALUE, WEBAC_MODE_WRITE_VALUE));
        roles.put("admin", Set.of(WEBAC_MODE_READ_VALUE, WEBAC_MODE_WRITE_VALUE));
        when(rolesProvider.getRoles(eq(resource), any(Transaction.class))).thenReturn(roles);
        final AuthorizationInfo authzinfo = webACAuthorizingRealm.doGetAuthorizationInfo(principalCollection);
        assertEquals(1, authzinfo.getRoles().size());
        assertTrue(authzinfo.getRoles().contains("fedoraUser"));
        assertNull(authzinfo.getStringPermissions());
        assertEquals(2, authzinfo.getObjectPermissions().size());
        assertTrue(authzinfo.getObjectPermissions().stream()
                .anyMatch(p -> p.implies(new WebACPermission(WEBAC_MODE_READ, requestUri))));
        assertTrue(authzinfo.getObjectPermissions().stream()
                .anyMatch(p -> p.implies(new WebACPermission(WEBAC_MODE_WRITE, requestUri))));
    }

    @Test
    public void testTransactionReturnsReadOnlyWhenNoAtomicIdHeader() throws Exception {
        // Ensure we have a proper web request bound (setUpShiro already bound the subject)
        when(request.getHeader(ATOMIC_ID_HEADER)).thenReturn(null);

        final var m = WebACAuthorizingRealm.class.getDeclaredMethod("transaction");
        m.setAccessible(true);

        final var tx = (Transaction) m.invoke(webACAuthorizingRealm);

        assertSame(org.fcrepo.kernel.api.ReadOnlyTransaction.INSTANCE, tx);
    }

    @AfterEach
    public void tearDownShiro() {
        ThreadContext.unbindSubject();
        ThreadContext.unbindSecurityManager();
    }
}
