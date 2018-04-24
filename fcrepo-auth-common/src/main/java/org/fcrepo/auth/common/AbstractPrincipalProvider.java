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

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;
import static org.slf4j.LoggerFactory.getLogger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.slf4j.Logger;

/**
 * This abstract class implements the Filter interface to add principals
 * to the session so that Shiro can evaluate the principals.
 * 
 * The getPrincipals method is left as the responsibility of the classes
 * that will be extending this abstract class.
 *
 * @author Mohamed Abdul Rasheed
 * @see PrincipalProvider
 */
abstract class AbstractPrincipalProvider implements PrincipalProvider {

    private static final Logger log = getLogger(AbstractPrincipalProvider.class);

    private static final String REALM_NAME = "org.fcrepo.auth.webac.WebACAuthorizingRealm";

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // this method intentionally left empty
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest hsRequest = (HttpServletRequest) request;
        final HttpSession session = hsRequest.getSession();
        PrincipalCollection principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
        final Set<Principal> newPrincipals = getPrincipals(hsRequest);
        if (newPrincipals.size() > 0) {
            final Set<Principal> currentPrincipals;
            if (principals == null) {
                log.debug("Shiro Principal object is not found in session!");
                currentPrincipals = newPrincipals;
              } else {
                currentPrincipals = new HashSet<Principal>((Set<Principal>) principals.asSet());
                log.debug("Number of Principals already in session object: {}", currentPrincipals.size());
                currentPrincipals.addAll(newPrincipals);
            }
            log.debug("Number of Principals after processing the current request: {}", currentPrincipals.size());
            principals = new SimplePrincipalCollection(currentPrincipals, REALM_NAME);
            hsRequest.getSession().setAttribute(PRINCIPALS_SESSION_KEY, principals);
        } else {
            log.debug("New Principals not found in the request!");
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // this method intentionally left empty
    }
}
