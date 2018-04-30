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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

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
    private Principal principal;

    @Mock
    private Principal everyone;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() {
        when(request.getUserPrincipal()).thenReturn(principal);
        when(everyone.getName()).thenReturn("http://xmlns.com/foaf/0.1/Agent");
    }

    @Test
    public void testIsNotAnonymous() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal);
        assertFalse(context.isAnonymous());
    }

    @Test
    public void testIsAnonymous() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(null);
        assertTrue(context.isAnonymous());
    }

    @Test
    public void testGetAnonymousUserName() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(null);
        assertEquals("http://xmlns.com/foaf/0.1/Agent", context.getUserName());
    }

    @Test
    public void testGetUserName() {
        when(principal.getName()).thenReturn("username");
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal);
        assertEquals("username", context.getUserName());
    }

    @Test
    public void testHasRole() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal);
        // Should always return true
        assertTrue(context.hasRole("anything"));
    }

    @Test(expected = NullPointerException.class)
    public void testHasPermissionNullActions() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal);

        context.hasPermission(null, null, (String[]) null);
    }

    @Test
    public void testHasPermission() {
        final FedoraUserSecurityContext context =
                new FedoraUserSecurityContext(principal);

        final Path path = mock(Path.class);

        assertTrue("Failed to grant register_namespace permission", context
                .hasPermission(null, null, new String[] {"register_namespace"}));

        assertTrue("Failed to grant register_type permission", context
                .hasPermission(null, null, new String[] {"register_type"}));

        // Grant all other permissions
        assertTrue("Granted write permission on root", context.hasPermission(
                null, path, new String[] {"read", "write"}));

        context.logout();
        assertFalse("Granted permission when the context was logged out",
                context.hasPermission(null, path, new String[] {"read"}));
    }

    @Test
    public void testHasPermissionBinary() {
        final FedoraUserSecurityContext context = new FedoraUserSecurityContext(principal);

        final Path path = mock(Path.class);

        assertTrue(context.hasPermission(mock(Context.class), path, "read"));
    }

}
