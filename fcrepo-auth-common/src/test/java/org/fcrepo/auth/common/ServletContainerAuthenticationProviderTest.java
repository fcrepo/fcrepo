/**
 * Copyright 2015 DuraSpace, Inc.
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

import static org.fcrepo.auth.common.FedoraAuthorizationDelegate.FEDORA_ALL_PRINCIPALS;
import static org.fcrepo.auth.common.ServletContainerAuthenticationProvider.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthenticationProvider.FEDORA_USER_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthenticationProvider.getInstance;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Session;
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
 * @since Feb 12, 2014
 */
public class ServletContainerAuthenticationProviderTest {

    @Mock
    private ServletCredentials creds;

    @Mock
    private FedoraAuthorizationDelegate fad;

    @Mock
    private Principal principal;

    @Mock
    private Principal everyone;

    @Mock
    private HttpServletRequest request;

    private Map<String, Object> sessionAttributes;

    @Captor
    private ArgumentCaptor<Set<Principal>> principalCaptor;

    private ExecutionContext context;

    @Before
    public void setUp() {
        initMocks(this);
        when(request.getUserPrincipal()).thenReturn(principal);
        when(fad.getEveryonePrincipal()).thenReturn(everyone);
        when(everyone.getName()).thenReturn("EVERYONE");
        when(creds.getRequest()).thenReturn(request);
        context = new ExecutionContext();
        sessionAttributes = new HashMap<>();
    }

    @Test
    public void testGetInstance() {
        final AuthenticationProvider provider = getInstance();

        assertNotNull(provider);

        final AuthenticationProvider secondProvider = getInstance();

        assertTrue(
                "Provider instance retrieved on second call should be the same object",
                provider == secondProvider);
    }

    @Test
    public void testInvalidCredentialsObject() {
        final AuthenticationProvider provider = getInstance();

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

        when(request.isUserInRole(FEDORA_ADMIN_ROLE)).thenReturn(true);

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

        provider.setFad(fad);

        when(request.isUserInRole(FEDORA_USER_ROLE)).thenReturn(true);

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

        verify(fad).hasPermission(any(Session.class), any(Path.class), any(String[].class));

        @SuppressWarnings("unchecked")
        final Set<Principal> resultPrincipals = (Set<Principal>) sessionAttributes.get(FEDORA_ALL_PRINCIPALS);

        assertEquals(2, resultPrincipals.size());
        assertTrue("EVERYONE principal must be present", resultPrincipals
                .contains(fad.getEveryonePrincipal()));
        assertTrue("User principal must be present", resultPrincipals
                .contains(principal));

        assertEquals(
                "Resulting security context must exist and belong to userName",
                "userName", result.getSecurityContext().getUserName());

        assertEquals(fad, provider.getFad());
    }

    @Test
    public void testAuthenticateWithPrincipalFactory() {
        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider.getInstance();

        provider.setFad(fad);

        when(request.isUserInRole(FEDORA_USER_ROLE)).thenReturn(true);

        final Set<Principal> groupPrincipals = new HashSet<>();
        final Principal groupPrincipal = mock(Principal.class);
        groupPrincipals.add(groupPrincipal);
        final HttpHeaderPrincipalProvider principalProvider = mock(HttpHeaderPrincipalProvider.class);
        when(principalProvider.getPrincipals(any(Credentials.class))).thenReturn(groupPrincipals);

        final Set<PrincipalProvider> providers = new HashSet<>();
        providers.add(principalProvider);

        provider.setPrincipalProviders(providers);

        final ExecutionContext result = provider.authenticate(creds, "repo", "workspace", context, sessionAttributes);

        final AdvancedAuthorizationProvider authProvider = (AdvancedAuthorizationProvider) result.getSecurityContext();
        final AdvancedAuthorizationProvider.Context authContext = mock(AdvancedAuthorizationProvider.Context.class);

        // Perform hasPermission on auth provider to capture result principals
        // from authenticate
        authProvider.hasPermission(authContext, mock(Path.class), new String[] {"read"});

        verify(fad).hasPermission(any(Session.class), any(Path.class), any(String[].class));

        @SuppressWarnings("unchecked")
        final Set<Principal> resultPrincipals = (Set<Principal>) sessionAttributes.get(FEDORA_ALL_PRINCIPALS);

        assertEquals(3, resultPrincipals.size());
        assertTrue("EVERYONE principal must be present", resultPrincipals
                .contains(fad.getEveryonePrincipal()));
        assertTrue("User principal must be present", resultPrincipals.contains(principal));
        assertTrue("Group Principal from factory must be present", resultPrincipals.contains(groupPrincipal));

        // getInstance returns a static provider so zero out internal set
        provider.setPrincipalProviders(new HashSet<PrincipalProvider>());
    }

    @Test
    public void testAuthenticateNoUserPrincipal() {

        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider.getInstance();

        provider.setFad(fad);

        when(request.getUserPrincipal()).thenReturn(null);

        evaluateDefaultAuthenticateCase(provider, 1);
    }

    @Test
    public void testAuthenticateUnrecognizedRole() {

        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider.getInstance();

        provider.setFad(fad);

        when(request.isUserInRole("unknownRole")).thenReturn(true);

        evaluateDefaultAuthenticateCase(provider, 2);
    }

    private void evaluateDefaultAuthenticateCase(final ServletContainerAuthenticationProvider provider,
            final int expected) {
        final ExecutionContext result = provider.authenticate(creds, "repo", "workspace", context, sessionAttributes);

        assertNotNull(result);

        final AdvancedAuthorizationProvider authProvider = (AdvancedAuthorizationProvider) result.getSecurityContext();
        final AdvancedAuthorizationProvider.Context authContext = mock(AdvancedAuthorizationProvider.Context.class);

        authProvider.hasPermission(authContext, mock(Path.class), new String[] {"read"});

        verify(fad).hasPermission(any(Session.class), any(Path.class), any(String[].class));

        @SuppressWarnings("unchecked")
        final Set<Principal> resultPrincipals = (Set<Principal>) sessionAttributes.get(FEDORA_ALL_PRINCIPALS);

        assertEquals(expected, resultPrincipals.size());
        assertTrue("EVERYONE principal must be present", resultPrincipals.contains(fad.getEveryonePrincipal()));

        final Iterator<Principal> iterator = resultPrincipals.iterator();
        boolean succeeds = false;

        while (iterator.hasNext()) {
            final String name = iterator.next().getName();

            if (name != null && name.equals(fad.getEveryonePrincipal().getName())) {
                succeeds = true;
            }
        }

        assertTrue("Expected to find: " + fad.getEveryonePrincipal().getName(), succeeds);
    }
}
