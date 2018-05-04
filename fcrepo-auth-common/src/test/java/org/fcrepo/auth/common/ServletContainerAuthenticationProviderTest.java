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
package org.fcrepo.auth.common;

import static org.fcrepo.auth.common.ServletContainerAuthenticationProvider.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthenticationProvider.getInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.security.AuthenticationProvider;

/**
 * @author bbpennel
 * @since Feb 12, 2014
 */
public class ServletContainerAuthenticationProviderTest {

    @Mock
    private ServletCredentials creds;

    @Mock
    private Principal principal;

    @Mock
    private Principal everyone;

    @Mock
    private HttpServletRequest request;

    @Mock
    private DelegateHeaderPrincipalProvider delegateProvider;

    @Mock
    private Principal delegatePrincipal;

    private Map<String, Object> sessionAttributes;

    @Captor
    private ArgumentCaptor<Set<Principal>> principalCaptor;

    private ExecutionContext context;

    @Before
    public void setUp() {
        initMocks(this);
        when(request.getUserPrincipal()).thenReturn(principal);
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
    public void testDelegatedAuthenticationForAdmins() {
        final ServletContainerAuthenticationProvider provider = (ServletContainerAuthenticationProvider) getInstance();
        provider.setPrincipalProviders(Collections.singleton(delegateProvider));

        when(request.isUserInRole(FEDORA_ADMIN_ROLE)).thenReturn(true);

        when(principal.getName()).thenReturn("adminName");

        when(delegateProvider.getDelegate(request)).thenReturn(delegatePrincipal);
        when(delegatePrincipal.getName()).thenReturn("delegatedUserName");

        final ExecutionContext result =
                provider.authenticate(creds, "repo", "workspace", context,
                        sessionAttributes);

        assertEquals(
                "Resulting security context must exist and belong to delegatedUserName",
                "delegatedUserName", result.getSecurityContext().getUserName());
    }

    @Test
    public void testNoDelegatedAuthenticationForUsers() {
        final ServletContainerAuthenticationProvider provider = (ServletContainerAuthenticationProvider) getInstance();
        provider.setPrincipalProviders(Collections.singleton(delegateProvider));

        when(request.isUserInRole(FEDORA_ADMIN_ROLE)).thenReturn(false);

        when(principal.getName()).thenReturn("userName");

        when(delegateProvider.getDelegate(request)).thenReturn(delegatePrincipal);
        when(delegatePrincipal.getName()).thenReturn("delegatedUserName");

        // delegateProvider being HeaderProvider returns the header content in getPrincipals regardless of logged user
        when(delegateProvider.getPrincipals(request)).thenReturn(Sets.newHashSet(delegatePrincipal));


        final ExecutionContext result =
                provider.authenticate(creds, "repo", "workspace", context,
                        sessionAttributes);

        assertEquals(
                "Resulting security context must exist and belong to userName (delegated user is ignored)",
                "userName", result.getSecurityContext().getUserName());

    }

    @Test
    public void testAuthenticateNoUserPrincipal() {

        final ServletContainerAuthenticationProvider provider =
                (ServletContainerAuthenticationProvider) ServletContainerAuthenticationProvider.getInstance();

        when(request.getUserPrincipal()).thenReturn(null);

        final ExecutionContext result =
                provider.authenticate(creds, "repo", "workspace", context,
                        sessionAttributes);

        assertEquals(
                "Resulting security context must exist and belong to userName (delegated user is ignored)",
                "http://xmlns.com/foaf/0.1/Agent", result.getSecurityContext().getUserName());
    }
}
