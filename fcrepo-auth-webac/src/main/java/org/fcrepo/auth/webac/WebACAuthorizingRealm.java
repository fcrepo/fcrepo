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
package org.fcrepo.auth.webac;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.slf4j.Logger;

/**
 * Authorization-only realm that performs authorization checks using WebAC ACLs stored in a Fedora repository. It
 * locates the ACL for the currently requested resource and parses the ACL RDF into a set of {@link WebACPermission}
 * instances.
 *
 * @author peichman
 */
public class WebACAuthorizingRealm extends AuthorizingRealm {

    private static final Logger log = getLogger(WebACAuthorizingRealm.class);

    private static final ContainerRolesPrincipal adminPrincipal = new ContainerRolesPrincipal("fedoraAdmin");

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
        final SimpleAuthorizationInfo authzInfo = new SimpleAuthorizationInfo();

        // if the user was assigned the "fedoraAdmin" container role, they get the
        // "fedoraAdmin" application role
        final ContainerRolesPrincipal containerRole = principals.oneByType(ContainerRolesPrincipal.class);
        if (containerRole != null && containerRole.equals(adminPrincipal)) {
            authzInfo.addRole("fedoraAdmin");
        }

        return authzInfo;
    }

    /**
     * This realm is authorization-only.
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token)
            throws AuthenticationException {
        return null;
    }

    /**
     * This realm is authorization-only.
     */
    @Override
    public boolean supports(final AuthenticationToken token) {
        return false;
    }
}
