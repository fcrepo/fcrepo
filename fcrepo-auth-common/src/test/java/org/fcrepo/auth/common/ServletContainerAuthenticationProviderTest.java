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

package org.fcrepo.auth.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.security.AdvancedAuthorizationProvider;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.modeshape.jcr.value.Path;

/**
 * @author bbpennel
 * @date Feb 12, 2014
 */
public class ServletContainerAuthenticationProviderTest {

    @Mock
    private ServletCredentials creds;

    @Mock
    private FedoraPolicyEnforcementPoint pep;

    @Mock
    private Principal principal;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Map<String, Object> sessionAttributes;

    @Captor
    private ArgumentCaptor<Set<Principal>> principalCaptor;

    private ExecutionContext context;

    @Before
    public void setUp() {
        initMocks(this);
        when(request.getUserPrincipal()).thenReturn(principal);
        when(creds.getRequest()).thenReturn(request);
        context = new ExecutionContext();
    }

    @Test
    public void testGetInstance() {
        final AuthenticationProvider provider =
                ServletContainerAuthenticationProvider.getInstance();

        assertNotNull(provider);

        final AuthenticationProvider secondProvider =
                ServletContainerAuthenticationProvider.getInstance();

        assertEquals(
                "Provider instance retrieved on second call should be the same object",
                provider, secondProvider);
    }

    @Test
    public void testInvalidCredentialsObject() {
        final AuthenticationProvider provider =
                ServletContainerAuthenticationProvider.getInstance();

        ExecutionContext result =
                provider.authenticate(null, "repo", "workspace", context,
                        sessionAttributes);
        assertNull(result);

        result =
                provider.authenticate(mock(Credentials.class), "repo",
                        "workspace", context, null);
        assertNull(result);
    }

    @Test
    public void testAuthenticateFedoraAdmin() {
        final AuthenticationProvider provider =
                ServletContainerAuthenticationProvider.getInstance();

        when(
                request.isUserInRole(ServletContainerAuthenticationProvider.FEDORA_ADMIN_ROLE))
                .thenReturn(true);

        when(principal.getName()).thenReturn("adminName");

        final ExecutionContext result =
                provider.authenticate(creds, "repo", "workspace", context,
                        sessionAttributes);

        assertEquals(
                "Resulting security context must exist and belong to adminName",
                "adminName", result.getSecurityContext().getUserName());
    }

    @Test
    public void testAuthenticateUserRole() {
        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider
                .getInstance();

        provider.setPep(pep);

        when(
                request.isUserInRole(ServletContainerAuthenticationProvider.FEDORA_USER_ROLE))
                .thenReturn(true);

        when(principal.getName()).thenReturn("userName");

        final ExecutionContext result =
                provider.authenticate(creds, "repo", "workspace", context,
                        sessionAttributes);

        assertNotNull(result);

        final AdvancedAuthorizationProvider authProvider =
                (AdvancedAuthorizationProvider) result.getSecurityContext();
        final AdvancedAuthorizationProvider.Context authContext =
                mock(AdvancedAuthorizationProvider.Context.class);

        // Perform hasPermission on auth provider to capture result principals
        // from authenticate
        authProvider.hasPermission(authContext, mock(Path.class),
                new String[] {"read"});

        verify(pep).hasModeShapePermission(any(Path.class),
                any(String[].class), principalCaptor.capture(),
                any(Principal.class));

        final Set<Principal> resultPrincipals = principalCaptor.getValue();

        assertEquals(2, resultPrincipals.size());
        assertTrue("EVERYONE principal must be present", resultPrincipals
                .contains(ServletContainerAuthenticationProvider.EVERYONE));
        assertTrue("User principal must be present", resultPrincipals
                .contains(principal));

        assertEquals(
                "Resulting security context must exist and belong to userName",
                "userName", result.getSecurityContext().getUserName());

        assertEquals(pep, provider.getPep());
    }

    @Test
    public void testAuthenticateWithPrincipalFactory() {
        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider
                .getInstance();

        provider.setPep(pep);

        when(
                request.isUserInRole(ServletContainerAuthenticationProvider.FEDORA_USER_ROLE))
                .thenReturn(true);

        final Set<Principal> groupPrincipals = new HashSet<>();
        final Principal groupPrincipal = mock(Principal.class);
        groupPrincipals.add(groupPrincipal);
        final HTTPPrincipalFactory principalFactory =
                mock(HTTPPrincipalFactory.class);
        when(principalFactory.getGroupPrincipals(any(HttpServletRequest.class)))
        .thenReturn(groupPrincipals);

        final Set<HTTPPrincipalFactory> factories = new HashSet<>();
        factories.add(principalFactory);

        provider.setPrincipalFactories(factories);

        final ExecutionContext result =
                provider.authenticate(creds, "repo", "workspace", context,
                        sessionAttributes);

        final AdvancedAuthorizationProvider authProvider =
                (AdvancedAuthorizationProvider) result.getSecurityContext();
        final AdvancedAuthorizationProvider.Context authContext =
                mock(AdvancedAuthorizationProvider.Context.class);

        // Perform hasPermission on auth provider to capture result principals
        // from authenticate
        authProvider.hasPermission(authContext, mock(Path.class),
                new String[] {"read"});

        verify(pep).hasModeShapePermission(any(Path.class),
                any(String[].class), principalCaptor.capture(),
                any(Principal.class));

        final Set<Principal> resultPrincipals = principalCaptor.getValue();

        assertEquals(3, resultPrincipals.size());
        assertTrue("EVERYONE principal must be present", resultPrincipals
                .contains(ServletContainerAuthenticationProvider.EVERYONE));
        assertTrue("User principal must be present", resultPrincipals
                .contains(principal));
        assertTrue("Group Principal from factory must be present",
                resultPrincipals.contains(groupPrincipal));
    }

    @Test
    public void testAuthenticateNoUserPrincipal() {

        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider
                .getInstance();

        provider.setPep(pep);

        when(request.getUserPrincipal()).thenReturn(null);

        evaluateDefaultAuthenticateCase(provider);
    }

    @Test
    public void testAuthenticateUnrecognizedRole() {

        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider
                .getInstance();

        provider.setPep(pep);

        when(request.isUserInRole("unknownRole")).thenReturn(true);

        evaluateDefaultAuthenticateCase(provider);
    }

    private void evaluateDefaultAuthenticateCase(
            final ServletContainerAuthenticationProvider provider) {
        final ExecutionContext result =
                provider.authenticate(creds, "repo", "workspace", context,
                        sessionAttributes);

        assertNotNull(result);

        final AdvancedAuthorizationProvider authProvider =
                (AdvancedAuthorizationProvider) result.getSecurityContext();
        final AdvancedAuthorizationProvider.Context authContext =
                mock(AdvancedAuthorizationProvider.Context.class);

        authProvider.hasPermission(authContext, mock(Path.class),
                new String[] {"read"});

        verify(pep).hasModeShapePermission(any(Path.class),
                any(String[].class), principalCaptor.capture(),
                any(Principal.class));

        final Set<Principal> resultPrincipals = principalCaptor.getValue();

        assertEquals(1, resultPrincipals.size());
        assertTrue("EVERYONE principal must be present", resultPrincipals
                .contains(ServletContainerAuthenticationProvider.EVERYONE));
        assertEquals(ServletContainerAuthenticationProvider.EVERYONE_NAME,
                resultPrincipals.iterator().next().getName());
    }
}
