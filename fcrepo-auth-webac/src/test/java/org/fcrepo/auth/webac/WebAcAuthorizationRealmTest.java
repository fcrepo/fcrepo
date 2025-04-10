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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import javax.servlet.http.HttpServletRequest;

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
}
