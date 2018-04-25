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

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_APPEND;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;

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

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.springframework.web.util.ContentCachingRequestWrapper;

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

    private boolean isAuthorized(final Subject currentUser, final HttpServletRequest httpRequest) throws IOException {
        final URI requestURI = URI.create(httpRequest.getRequestURL().toString());
        switch (httpRequest.getMethod()) {
        case "GET":
            return currentUser.isPermitted(new WebACPermission(WEBAC_MODE_READ, requestURI));
        case "PUT":
        case "POST":
        case "DELETE":
            return currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, requestURI));
        case "PATCH":
            if (currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, requestURI))) {
                return true;
            } else {
                if (currentUser.isPermitted(new WebACPermission(WEBAC_MODE_APPEND, requestURI))) {
                    return isPatchContentPermitted(httpRequest);
                }
            }
            return false;
        default:
            return false;
        }
    }

    private boolean isPatchContentPermitted(final HttpServletRequest httpRequest) throws IOException {
        if (!contentTypeSPARQLUpdate.equalsIgnoreCase(httpRequest.getContentType())) {
            log.debug("Cannot verify authorization on NON-SPARQL Patch request.");
            return false;
        }
        if (httpRequest.getInputStream() != null) {
            final ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(httpRequest);
            boolean noDeletes = false;
            try {
                noDeletes = !hasDeleteClause(IOUtils.toString(cachedRequest.getInputStream(), UTF_8));
            } catch (QueryParseException ex) {
                log.error("Cannot verify authorization! Exception while inspecting SPARQL query!", ex);
            }
            return noDeletes;
        } else {
            log.debug("Authorizing SPARQL request with no content.");
            return true;
        }
    }

    private boolean hasDeleteClause(final String sparqlString) {
        final UpdateRequest sparqlUpdate = UpdateFactory.create(sparqlString);
        return sparqlUpdate.getOperations().stream().filter(update -> (update instanceof UpdateModify))
                .peek(update -> log.debug("Inspecting update statement for DELETE clause: {}", update.toString()))
                .map(update -> (UpdateModify)update)
                .anyMatch(UpdateModify::hasDeleteClause);
    }
}
