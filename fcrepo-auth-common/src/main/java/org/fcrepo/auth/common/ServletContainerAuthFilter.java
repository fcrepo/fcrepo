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

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;

/**
 * @author peichman
 */
public class ServletContainerAuthFilter implements Filter {

    private static final Logger log = getLogger(ServletContainerAuthFilter.class);

    // TODO: configurable set of role names
    private static final String[] ROLE_NAMES = { "fedoraAdmin", "fedoraUser" };

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final Principal servletUser = httpRequest.getUserPrincipal();
        if (servletUser != null) {
            log.info("there is a servlet user: " + servletUser.getName());
            final Set<String> roles = new HashSet<String>();
            for (String roleName : ROLE_NAMES) {
                log.info("testing role " + roleName);
                if (httpRequest.isUserInRole(roleName)) {
                    log.info("servlet user has servlet role: " + roleName);
                    roles.add(roleName);
                }
            }
            final ContainerAuthToken token = new ContainerAuthToken(servletUser.getName(), roles);
            log.info("credentials for servletUser = " + token.getCredentials().toString());
            final Subject currentUser = SecurityUtils.getSubject();
            currentUser.login(token);
        } else {
            log.info("anonymous request");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
