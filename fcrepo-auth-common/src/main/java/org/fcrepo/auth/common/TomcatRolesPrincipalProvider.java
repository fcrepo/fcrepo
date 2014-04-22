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

package org.fcrepo.auth.common;

import static java.util.Collections.emptySet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

import org.modeshape.jcr.api.ServletCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that uses the roles configured in Tomcat's MemoryUser as principals for
 * access.
 *
 * @author <a href="mailto:ksclarke@gmail.com">Kevin S. Clarke</a>
 */
public class TomcatRolesPrincipalProvider implements PrincipalProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatRolesPrincipalProvider.class);

    private static final String MEMORY_USER = "org.apache.catalina.users.MemoryUser";

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.PrincipalProvider#getPrincipals(javax.jcr.Credentials)
     */
    @Override
    public Set<Principal> getPrincipals(final Credentials credentials) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking for principals using {}", TomcatRolesPrincipalProvider.class.getSimpleName());
        }

        if (!(credentials instanceof ServletCredentials)) {
            return emptySet();
        }

        final ServletCredentials servletCredentials = (ServletCredentials) credentials;
        final HttpServletRequest request = servletCredentials.getRequest();

        if (request == null) {
            return emptySet();
        }

        final Principal principal = request.getUserPrincipal();
        final Set<Principal> principals = new HashSet<>();

        if (principal == null) {
            return emptySet();
        }

        final Class<?> principalClass = principal.getClass();
        final String className = principalClass.getName();

        // Use reflection because we don't want to hard-code container specifics into the project.
        if (className.equals(MEMORY_USER)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking Catalina configured roles for '{}'", principal.getName());
            }

            try {
                final Method method = principalClass.getMethod("getRoles");
                @SuppressWarnings("unchecked")
                final Iterator<Principal> iterator = (Iterator<Principal>) method.invoke(principal);

                while (iterator.hasNext()) {
                    principals.add(iterator.next());
                }
            } catch (final NoSuchMethodException e) {
                LOGGER.error("Didn't find expected Catalina MemoryUser getRoles() method", e);
                return emptySet();
            } catch (final InvocationTargetException e) {
                LOGGER.error("Wasn't able to invoke Catalina MemoryUser's getRoles() method", e);
                return emptySet();
            } catch (final IllegalAccessException e) {
                LOGGER.error("Illegal access to Catalina MemoryUser's getRoles() method", e);
                return emptySet();
            } catch (final ClassCastException e) {
                LOGGER.error("Could not cast the expected return type from Catalina MemoryUser's getRoles() method");
                return emptySet();
            }
        } else {
            return emptySet();
        }

        return principals;
    }

}
