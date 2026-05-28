/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;

/**
 * @author peichman
 */
public class ServletContainerAuthFilter implements Filter {

    private static final Logger log = getLogger(ServletContainerAuthFilter.class);

    /**
     * User role for Fedora's admin users
     */
    public static final String FEDORA_ADMIN_ROLE = "fedoraAdmin";

    /**
     * User role for Fedora's ordinary users
     */
    public static final String FEDORA_USER_ROLE = "fedoraUser";

    // TODO: configurable set of role names: https://jira.duraspace.org/browse/FCREPO-2770
    private static final String[] ROLE_NAMES = { FEDORA_ADMIN_ROLE, FEDORA_USER_ROLE };

    @Override
    public void init(final FilterConfig filterConfig) {
        // this method intentionally left empty
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final Principal servletUser = httpRequest.getUserPrincipal();
        final Subject currentUser = SecurityUtils.getSubject();

        if (servletUser != null) {
            log.debug("There is a servlet user: {}", servletUser.getName());
            final Set<String> roles = new HashSet<>();
            for (final String roleName : ROLE_NAMES) {
                log.debug("Testing role {}", roleName);
                if (httpRequest.isUserInRole(roleName)) {
                    log.debug("Servlet user {} has servlet role: {}", servletUser.getName(), roleName);
                    roles.add(roleName);
                }
            }
            final ContainerAuthToken token = new ContainerAuthToken(servletUser.getName(), roles);
            log.debug("Credentials for servletUser = {}", token.getCredentials());
            currentUser.login(token);
        } else {
            log.debug("Anonymous request");
            // ensure the user is actually logged out
            currentUser.logout();
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // this method intentionally left empty
    }

}
