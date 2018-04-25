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

import static java.util.Collections.emptySet;

import javax.servlet.http.HttpServletRequest;

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
    public static class ContainerRolesPrincipal implements Principal {

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
     * org.fcrepo.auth.PrincipalProvider#getPrincipals(javax.servlet.http.HttpServletRequest)
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
