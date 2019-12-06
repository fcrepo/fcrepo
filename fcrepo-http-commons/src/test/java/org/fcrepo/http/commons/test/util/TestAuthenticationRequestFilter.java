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
package org.fcrepo.http.commons.test.util;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Base64;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.glassfish.grizzly.http.server.GrizzlyPrincipal;
import org.slf4j.Logger;

/**
 * @author Gregory Jansen
 */
public class TestAuthenticationRequestFilter implements Filter {

    private static final Logger log = getLogger(TestAuthenticationRequestFilter.class);

    private static final String FEDORA_ADMIN_USER = "fedoraAdmin";

    /*
     * (non-Javadoc)
     * @see
     * com.sun.jersey.spi.container.ContainerRequestFilter#filter(com.sun.jersey
     * .spi.container.ContainerRequest)
     */
    @Override
    public void doFilter(final ServletRequest request,
            final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final String username = getUsername(req);
        // Validate the extracted credentials
        Set<String> containerRoles = emptySet();
        if (username == null) {
            log.debug("ANONYMOUS");
        } else if (FEDORA_ADMIN_USER.equals(username)) {
            containerRoles = singleton("fedoraAdmin");
            log.debug("ADMIN AUTHENTICATED");
        } else if ("noroles".equals(username)) {
            log.debug("USER (without roles); AUTHENTICATED");
        } else {
            containerRoles = singleton("fedoraUser");
            log.debug("USER AUTHENTICATED");
        }
        final ServletRequest proxy = proxy(req, username, containerRoles);
        chain.doFilter(proxy, response);
    }

    private static ServletRequest proxy(final HttpServletRequest request,
            final String username, final Set<String> containerRoles) {
        final Principal user = username != null ? new GrizzlyPrincipal(username) : null;
        final HttpServletRequest result =
                (HttpServletRequest) newProxyInstance(request.getClass()
                        .getClassLoader(),
                        new Class[] {HttpServletRequest.class},
                        new InvocationHandler() {

                            @Override
                            public Object invoke(final Object proxy,
                                    final Method method, final Object[] args)
                                    throws Throwable {
                                if (method.getName().equals("isUserInRole")) {
                                    final String role = (String) args[0];
                                    return containerRoles.contains(role);
                                } else if (method.getName().equals(
                                        "getUserPrincipal")) {
                                    return user;
                                } else if (method.getName().equals(
                                        "getRemoteUser")) {
                                    return username;
                                }
                                return method.invoke(request, args);
                            }
                        });
        return result;
    }

    private static String getUsername(final HttpServletRequest request) {
        // Extract authentication credentials
        String authentication = request.getHeader(AUTHORIZATION);
        if (authentication == null) {
            return null;
        }
        if (!authentication.startsWith("Basic ")) {
            return null;
        }
        authentication = authentication.substring("Basic ".length());
        final String[] values = new String(Base64.getDecoder().decode(authentication)).split(":");
        if (values.length < 2) {
            throw new WebApplicationException(400);
            // "Invalid syntax for username and password"
        }
        final String username = values[0];
        final String password = values[1];
        if ((username == null) || (password == null)) {
            return null;
        }
        return username;
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(final FilterConfig filterConfig) {
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }
}
