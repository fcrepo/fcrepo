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

import java.util.HashSet;
import java.util.Set;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.shiro.authc.AuthenticationToken;
import org.fcrepo.auth.common.ContainerRolesPrincipalProvider.ContainerRolesPrincipal;
import org.slf4j.Logger;

/**
 * @author peichman
 */
public class ContainerAuthToken implements AuthenticationToken {

    private static final Logger log = getLogger(ContainerAuthToken.class);

    public static final String AUTHORIZED = "AUTHORIZED";

    private final BasicUserPrincipal servletUser;

    private final Set<ContainerRolesPrincipal> servletRoles;

    /**
     * @param servletUsername username returned from servlet container authentication
     * @param servletRoleNames roles returned from servlet container authentication
     */
    public ContainerAuthToken(final String servletUsername, final Set<String> servletRoleNames) {
        servletUser = new BasicUserPrincipal(servletUsername);
        log.debug("Setting servlet username {}", servletUsername);
        this.servletRoles = new HashSet<>();
        for (final String roleName : servletRoleNames) {
            log.debug("Adding servlet role {} to {}", roleName, servletUsername);
            this.servletRoles.add(new ContainerRolesPrincipal(roleName));
        }
    }

    @Override
    public Object getPrincipal() {
        return servletUser;
    }

    /**
     * This token represents a user who was already authenticated by the servlet container, so return a constant
     * credentials string.
     */
    @Override
    public Object getCredentials() {
        return AUTHORIZED;
    }

    /**
     * @return set of principals
     */
    public Set<ContainerRolesPrincipal> getRoles() {
        return servletRoles;
    }

}
