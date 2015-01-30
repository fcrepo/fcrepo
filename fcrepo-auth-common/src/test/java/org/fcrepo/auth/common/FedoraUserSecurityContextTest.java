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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.security.AdvancedAuthorizationProvider.Context;
import org.modeshape.jcr.value.Path;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import java.security.Principal;

/**
 * @author bbpennel
 * @since Feb 12, 2014
 */
public class FedoraUserSecurityContextTest {

    @Mock
    private FedoraAuthorizationDelegate fad;

    @Mock
    private Principal principal;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() {
        initMocks(this);
        when(request.getUserPrincipal()).thenReturn(principal);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testNoFAD() {
        new FedoraUserSecurityContext(principal, null);
    }

    @Test
    public void testIsNotAnonymous() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal, fad);
        assertFalse(context.isAnonymous());
    }

    public void testIsAnonymous() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(null, fad);
        assertTrue(context.isAnonymous());
    }

    @Test
    public void testGetEffectiveUserPrincipal() {
        FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal, fad);

        assertEquals("Effective user principal must match given principal",
                principal, context.getEffectiveUserPrincipal());

        context.logout();
        assertEquals("User principal when logged out should be EVERYONE",
                ServletContainerAuthenticationProvider.EVERYONE, context
                .getEffectiveUserPrincipal());

        context = new FedoraUserSecurityContext(null, fad);

        assertEquals(
                "Effective user principal should be EVERYONE when none is provided",
                ServletContainerAuthenticationProvider.EVERYONE, context
                .getEffectiveUserPrincipal());
    }

    @Test
    public void testGetAnonymousUserName() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(null, fad);
        assertEquals(ServletContainerAuthenticationProvider.EVERYONE.getName(),
                context.getUserName());
    }

    @Test
    public void testGetUserName() {
        when(principal.getName()).thenReturn("username");
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal, fad);
        assertEquals("username", context.getUserName());
    }

    @Test
    public void testHasRole() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal, fad);

        assertTrue(context.hasRole("read"));
        assertTrue(context.hasRole("write"));
        assertTrue(context.hasRole("admin"));
        assertFalse(context.hasRole(null));
        assertFalse(context.hasRole("other"));
    }

    @Test(expected = NullPointerException.class)
    public void testHasPermissionNullActions() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal, fad);

        context.hasPermission(null, null, (String[]) null);
    }

    @Test
    public void testHasPermission() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal, fad);

        assertFalse("Granted permission with no action on root", context
                .hasPermission(null, null, new String[] {}));

        assertFalse("Granted write permission on root", context.hasPermission(
                null, null, new String[] {"write"}));

        assertTrue("Failed to granted read permission on root", context
                .hasPermission(null, null, new String[] {"read"}));

        assertFalse("Granted write permission on root", context.hasPermission(
                null, null, new String[] {"read", "write"}));

        when(fad.hasPermission(any(Session.class), any(Path.class), any(String[].class))).thenReturn(true);

        final Path path = mock(Path.class);
        assertTrue(context.hasPermission(mock(Context.class), path, new String[] {"read"}));
        verify(fad).hasPermission(any(Session.class), any(Path.class), any(String[].class));

        context.logout();
        assertFalse("Granted permission when the context was logged out",
                context.hasPermission(null, path, new String[] {"read"}));
    }
}
