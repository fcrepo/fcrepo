/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for {@link ContainerRolesPrincipalProvider}.
 *
 * @author Kevin S. Clarke
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ContainerRolesPrincipalProviderTest {

    @Mock
    private HttpServletRequest request;

    private ContainerRolesPrincipalProvider provider;

    /**
     * Sets up ContainerRolesPrincipalProviderTest's tests.
     */
    @BeforeEach
    public void setUp() {
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
        assertTrue(principals.contains(new ContainerRolesPrincipal("a")), "The principals should contain 'a'");
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
        assertTrue(principals.contains(new ContainerRolesPrincipal("a")), "The principals should contain 'a'");
        assertTrue(principals.contains(new ContainerRolesPrincipal("b")), "The principals should contain 'b'");
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
        assertTrue(principals.contains(new ContainerRolesPrincipal("a")), "The principals should contain 'a'");
        assertTrue(principals.contains(new ContainerRolesPrincipal("b")), "The principals should contain 'b'");
    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#setRoleNames(Set)}.
     */
    @Test
    public void testNoConfigedRoleNames() {
        final Set<Principal> principals = provider.getPrincipals(request);
        assertTrue(principals.isEmpty(), "Empty set expected when no role names configured");
    }

    /**
     * Test for {@link ContainerRolesPrincipalProvider#getPrincipals(javax.servlet.http.HttpServletRequest)}.
     */
    @Test
    public void testNoRequest() {
        provider.setRoleNames(newHashSet("a"));

        final Set<Principal> principals = provider.getPrincipals(null);
        assertTrue(principals.isEmpty(), "Empty set expected when no request supplied");

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
        final Principal principalWithDifferentClass = mock(Principal.class);
        when(principalWithDifferentClass.getName()).thenReturn("a");

        // This test is verifying that the .equals method rejects any principal class other than HttpHeaderPrincipal
        assertFalse(principal.equals(principalWithDifferentClass),
                "Principals should not be equal if not the same class");
    }

}
