/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.slf4j.Logger;

/**
 * @author peichman
 */
public class ServletContainerAuthenticatingRealm extends AuthenticatingRealm {

    private static final Logger log = getLogger(ServletContainerAuthenticatingRealm.class);

    @Override
    public String getName() {
        return "servlet container authentication";
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
            throws AuthenticationException {
        final ContainerAuthToken authToken = (ContainerAuthToken) token;
        final SimplePrincipalCollection principals = new SimplePrincipalCollection();
        log.debug("Creating principals from servlet container principal and roles");
        // container-managed auth username
        principals.add(authToken.getPrincipal(), getName());
        // container-managed auth roles
        final Set<ContainerRolesPrincipal> roles = authToken.getRoles();
        if (!roles.isEmpty()) {
            principals.addAll(roles, getName());
        }
        return new SimpleAuthenticationInfo(principals, ContainerAuthToken.AUTHORIZED);
    }

    @Override
    public boolean supports(final AuthenticationToken token) {
        return token instanceof ContainerAuthToken;
    }

}
