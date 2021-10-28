/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.slf4j.LoggerFactory.getLogger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
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
    public void init(final FilterConfig filterConfig) {
        // this method intentionally left empty
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest hsRequest = (HttpServletRequest) request;
        final Subject currentUser = SecurityUtils.getSubject();
        PrincipalCollection principals = currentUser.getPrincipals();

        final Set<Principal> newPrincipals = getPrincipals(hsRequest);
        if (newPrincipals.size() > 0) {
            final Set<Principal> currentPrincipals;
            if (principals == null || principals.asList().isEmpty()) {
                log.debug("Shiro Principal object is not found!");
                currentPrincipals = newPrincipals;
              } else {
                currentPrincipals = new HashSet<>(principals.asSet());
                log.debug("Number of Principals already in session object: {}", currentPrincipals.size());
                currentPrincipals.addAll(newPrincipals);
            }
            log.debug("Number of Principals after processing the current request: {}", currentPrincipals.size());
            principals = new SimplePrincipalCollection(currentPrincipals, REALM_NAME);
            currentUser.runAs(principals);
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
