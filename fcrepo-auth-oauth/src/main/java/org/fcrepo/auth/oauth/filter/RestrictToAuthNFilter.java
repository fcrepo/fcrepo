/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.auth.oauth.filter;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

/**
 * @author ajs6f
 * @date Jul 1, 2013
 */
public class RestrictToAuthNFilter implements Filter {

    private static final Logger LOGGER = getLogger(RestrictToAuthNFilter.class);

    private static final String AUTHENTICATED_SECTION = "/authenticated/";

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        init();
    }

    @PostConstruct
    public void init() {
        LOGGER.debug("Initialized {}", this.getClass().getName());
    }

    /*
     * (non-Javadoc) Assumes that the filter chain contains {@link
     * org.apache.oltu.oauth2.rsfilter.OAuthFilter} in a preceding position.
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(final ServletRequest request,
            final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;
        final String requestURI = req.getRequestURI();
        LOGGER.debug("Received request at URI: {}", requestURI);
        if (requestURI.contains(AUTHENTICATED_SECTION)) {
            // a protected resource
            LOGGER.debug("{} is a protected resource.", requestURI);
            if (req.getUserPrincipal() != null) {
                LOGGER.debug("Couldn't find authenticated user!");
                res.sendError(SC_UNAUTHORIZED);
            } else {
                LOGGER.debug("Found authenticated user.");
                chain.doFilter(request, response);
            }
        } else {
            // not a protected resource
            LOGGER.debug("{} is not a protected resource.", requestURI);
            chain.doFilter(request, response);
        }

    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroyed {}", this.getClass().getName());

    }

}
