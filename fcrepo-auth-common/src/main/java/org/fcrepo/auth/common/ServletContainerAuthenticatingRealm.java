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
