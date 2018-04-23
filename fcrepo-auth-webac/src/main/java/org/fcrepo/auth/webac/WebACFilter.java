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
import org.apache.shiro.session.Session;
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
        final Session userSession = currentUser.getSession();

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final URI requestURI = URI.create(httpRequest.getRequestURL().toString());
        userSession.setAttribute("requestURI", requestURI);
        if (currentUser.isAuthenticated()) {
            log.debug("User is authenticated");
            if (currentUser.hasRole("fedoraAdmin")) {
                log.debug("User has fedoraAdmin role");
            } else {
                log.debug("User does NOT have fedoraAdmin role");
                // non-admins are subject to permission checks

                // TODO: permission checks based on the request: https://jira.duraspace.org/browse/FCREPO-2762
                // e.g. currentUser.isPermitted(new WebACPermission(WEBAC_MODE_READ, requestURI))

                // otherwise, set response to forbidden
                ((HttpServletResponse) response).sendError(403);
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

}
