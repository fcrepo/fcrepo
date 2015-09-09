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

import static org.fcrepo.kernel.api.FedoraJcrTypes.JCR_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.security.AdvancedAuthorizationProvider.Context;
import org.modeshape.jcr.value.Path;

/**
 * @author bbpennel
 * @since Feb 12, 2014
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraUserSecurityContextTest {

    @Mock
    private FedoraAuthorizationDelegate fad;

    @Mock
    private Principal principal;

    @Mock
    private Principal everyone;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() {
        when(request.getUserPrincipal()).thenReturn(principal);
        when(fad.getEveryonePrincipal()).thenReturn(everyone);
        when(everyone.getName()).thenReturn("EVERYONE");
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

    @Test
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
                fad.getEveryonePrincipal(), context
                .getEffectiveUserPrincipal());

        context = new FedoraUserSecurityContext(null, fad);

        assertEquals(
                "Effective user principal should be EVERYONE when none is provided",
                fad.getEveryonePrincipal(), context
                .getEffectiveUserPrincipal());
    }

    @Test
    public void testGetAnonymousUserName() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(null, fad);
        assertEquals(fad.getEveryonePrincipal().getName(),
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
        final Path.Segment segment = mock(Path.Segment.class);
        when(path.getLastSegment()).thenReturn(segment);
        when(segment.getString()).thenReturn("junk");
        assertTrue(context.hasPermission(mock(Context.class), path, new String[] {"read"}));
        verify(fad).hasPermission(any(Session.class), any(Path.class), any(String[].class));

        context.logout();
        assertFalse("Granted permission when the context was logged out",
                context.hasPermission(null, path, new String[] {"read"}));
    }

    @Test
    public void testHasPermissionBinary() {
        final FedoraUserSecurityContext context = new FedoraUserSecurityContext(principal, fad);

        final Path path = mock(Path.class);
        final Path.Segment segment = mock(Path.Segment.class);
        when(path.getLastSegment()).thenReturn(segment);
        when(segment.getString()).thenReturn(JCR_CONTENT);
        when(path.size()).thenReturn(2);
        when(path.subpath(0, 1)).thenReturn(path);

        when(fad.hasPermission(any(Session.class), any(Path.class), any(String[].class))).thenReturn(true);

        assertTrue(context.hasPermission(mock(Context.class), path, "read"));
    }

}
