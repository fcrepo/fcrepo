/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.shiro.authc.AuthenticationInfo;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.junit.jupiter.api.Test;

/**
 * @author peichman
 */
public class ServletContainerAuthenticatingRealmTest {

    @Test
    public void testDoGetAuthenticationInfo() {
        final String username = "foo";
        final String roleName = "fedoraUser";

        final ServletContainerAuthenticatingRealm realm = new ServletContainerAuthenticatingRealm();
        final Set<String> roles = new HashSet<>();
        roles.add(roleName);
        final ContainerAuthToken token = new ContainerAuthToken(username, roles);
        final AuthenticationInfo info = realm.doGetAuthenticationInfo(token);
        // should have 2 principals (user and one role)
        assertEquals(2, info.getPrincipals().asSet().size());
        assertEquals(1, info.getPrincipals().byType(BasicUserPrincipal.class).size());
        assertEquals(1, info.getPrincipals().byType(ContainerRolesPrincipal.class).size());

        assertEquals(username, info.getPrincipals().byType(BasicUserPrincipal.class).toArray(
                new BasicUserPrincipal[1])[0].getName());
        assertEquals(roleName, info.getPrincipals().byType(ContainerRolesPrincipal.class).toArray(
                new ContainerRolesPrincipal[1])[0].getName());
    }

    @Test
    public void testDoGetAuthenticationInfoWithNoRoles() {
        final String username = "foo";

        final ServletContainerAuthenticatingRealm realm = new ServletContainerAuthenticatingRealm();
        final ContainerAuthToken token = new ContainerAuthToken(username, new HashSet<>());
        // make sure this doesn't blow up on an empty set of roles
        final AuthenticationInfo info = realm.doGetAuthenticationInfo(token);
        // should have 1 principal (user)
        assertEquals(1, info.getPrincipals().asSet().size());
        assertEquals(1, info.getPrincipals().byType(BasicUserPrincipal.class).size());
        assertEquals(0, info.getPrincipals().byType(ContainerRolesPrincipal.class).size());

        assertEquals(username, info.getPrincipals().byType(BasicUserPrincipal.class).toArray(
                new BasicUserPrincipal[1])[0].getName());
    }

}
