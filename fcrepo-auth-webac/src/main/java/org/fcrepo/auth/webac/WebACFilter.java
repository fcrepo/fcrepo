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

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;

/**
 * @author peichman
 */
public class WebACFilter implements Filter {

    private static final Logger log = getLogger(WebACFilter.class);

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // this method intentionally left empty
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final Subject currentUser = SecurityUtils.getSubject();

        if (currentUser.isAuthenticated()) {
            log.debug("User is authenticated");
            if (currentUser.hasRole(FEDORA_ADMIN_ROLE)) {
                log.debug("User has fedoraAdmin role");
            } else if (currentUser.hasRole(FEDORA_USER_ROLE)) {
                log.debug("User has fedoraUser role");
                // non-admins are subject to permission checks
                final HttpServletRequest httpRequest = (HttpServletRequest) request;
                if (!isAuthorized(currentUser, httpRequest)) {
                    // if the user is not authorized, set response to forbidden
                    ((HttpServletResponse) response).sendError(SC_FORBIDDEN);
                }
            } else {
                log.debug("User has no recognized servlet container role");
                // missing a container role, return forbidden
                ((HttpServletResponse) response).sendError(SC_FORBIDDEN);
            }
        } else {
            log.debug("User is NOT authenticated");
        }

        // proceed to the next filter
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // this method intentionally left empty
    }

    private boolean isAuthorized(final Subject currentUser, final HttpServletRequest httpRequest) {
        final URI requestURI = URI.create(httpRequest.getRequestURL().toString());
        switch (httpRequest.getMethod()) {
        case "GET":
            return currentUser.isPermitted(new WebACPermission(WEBAC_MODE_READ, requestURI));
        case "PUT":
        case "POST":
        case "DELETE":
        case "PATCH":
            return currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, requestURI));
        default:
            return false;
        }
    }

}
