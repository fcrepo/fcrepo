/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static java.util.Collections.emptySet;

import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds principals based on roles that are configured within the container and
 * through a PrincipalProvider bean in the project.
 *
 * @author Kevin S. Clarke
 * @see PrincipalProvider
 */
public class ContainerRolesPrincipalProvider extends AbstractPrincipalProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRolesPrincipalProvider.class);

    /**
     * @author Kevin S. Clarke
     */
    public static class ContainerRolesPrincipal implements Principal, Serializable {

        private final String name;

        /**
         * @param name principal name
         */
        public ContainerRolesPrincipal(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof ContainerRolesPrincipal) {
                return ((ContainerRolesPrincipal) o).getName().equals(this.getName());
            }

            return false;
        }

        @Override
        public int hashCode() {
            if (name == null) {
                return 0;
            }

            return name.hashCode();
        }

    }

    private Set<String> roleNames;

    /**
     * Sets the role names which have been configured in the repo.xml file.
     *
     * @param roleNames The names of container roles that should be recognized
     *        as principals
     */
    public void setRoleNames(final Set<String> roleNames) {
        this.roleNames = roleNames;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.PrincipalProvider#getPrincipals(jakarta.servlet.http.HttpServletRequest)
     */
    @Override
    public Set<Principal> getPrincipals(final HttpServletRequest request) {
        LOGGER.debug("Checking for principals using {}", ContainerRolesPrincipalProvider.class.getSimpleName());

        if (request == null) {
            LOGGER.debug("Servlet request was null");

            return emptySet();
        }

        if (roleNames == null) {
            LOGGER.debug("Role names Set was never initialized");

            return emptySet();
        }

        final Iterator<String> iterator = roleNames.iterator();
        final Set<Principal> principals = new HashSet<>();

        while (iterator.hasNext()) {
            final String role = iterator.next().trim();

            if (request.isUserInRole(role)) {
                LOGGER.debug("Adding container role as principal: {}", role);

                principals.add(new ContainerRolesPrincipal(role));
            }
        }

        return principals;
    }

}
