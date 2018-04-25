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

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Tests for {@link ContainerRolesPrincipalProvider}.
 *
 * @author Kevin S. Clarke
 */
public class ContainerRolesPrincipalProviderTest {

    @Mock
    private HttpServletRequest request;

    private ContainerRolesPrincipalProvider provider;

    /**
     * Sets up ContainerRolesPrincipalProviderTest's tests.
     */
    @Before
    public void setUp() {
        initMocks(this);
        provider = new ContainerRolesPrincipalProvider();
    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#setRoleNames(Set)}.
     */
    @Test
    public void testSetRole() {
        when(request.isUserInRole("a")).thenReturn(true);
        provider.setRoleNames(newHashSet("a"));

        final Set<Principal> principals = provider.getPrincipals(request);

        assertEquals(1, principals.size());
        assertTrue("The principals should contain 'a'", principals.contains(new ContainerRolesPrincipal("a")));
    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#setRoleNames(Set)}.
     */
    @Test
    public void testSetRoles() {
        when(request.isUserInRole("a")).thenReturn(true);
        when(request.isUserInRole("b")).thenReturn(true);
        provider.setRoleNames(newHashSet("a", "b"));

        final Set<Principal> principals = provider.getPrincipals(request);

        assertEquals(2, principals.size());
        assertTrue("The principals should contain 'a'", principals.contains(new ContainerRolesPrincipal("a")));
        assertTrue("The principals should contain 'b'", principals.contains(new ContainerRolesPrincipal("b")));
    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#setRoleNames(Set)}.
     */
    @Test
    public void testTrimSetRoles() {
        when(request.isUserInRole("a")).thenReturn(true);
        when(request.isUserInRole("b")).thenReturn(true);
        provider.setRoleNames(newHashSet(" a", "b "));

        final Set<Principal> principals = provider.getPrincipals(request);

        assertEquals(2, principals.size());
        assertTrue("The principals should contain 'a'", principals.contains(new ContainerRolesPrincipal("a")));
        assertTrue("The principals should contain 'b'", principals.contains(new ContainerRolesPrincipal("b")));
    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#setRoleNames(Set)}.
     */
    @Test
    public void testNoConfigedRoleNames() {
        final Set<Principal> principals = provider.getPrincipals(request);
        assertTrue("Empty set expected when no role names configured", principals.isEmpty());
    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#getPrincipals(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testNoRequest() {
        provider.setRoleNames(newHashSet("a"));

        final Set<Principal> principals = provider.getPrincipals(null);
        assertTrue("Empty set expected when no request supplied", principals.isEmpty());

    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#getPrincipals(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testPrincipalEqualsDifferentClass() {
        when(request.isUserInRole("a")).thenReturn(true);
        provider.setRoleNames(newHashSet("a"));

        final Set<Principal> principals = provider.getPrincipals(request);
        final Principal principal = principals.iterator().next();

        assertNotEquals("Principals should not be equal if not the same class", principal, mock(Principal.class));
    }

}
