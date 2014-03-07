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

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Authenticates ModeShape logins where JAX-RS credentials are supplied. Capable
 * of authenticating whether or not container has performed user authentication.
 * This is a singleton with an injected policy enforcement point. The singleton
 * pattern allows ModeShape to obtain this instance via classname configuration.
 *
 * @author Gregory Jansen
 */
public class ServletContainerAuthenticationProvider implements
        AuthenticationProvider {

    private static ServletContainerAuthenticationProvider _instance = null;

    private ServletContainerAuthenticationProvider() {
        _instance = this;
    }

    public static final String EVERYONE_NAME = "EVERYONE";

    /**
     * The security principal for every request.
     */
    public static final Principal EVERYONE = new Principal() {

        @Override
        public String getName() {
            return ServletContainerAuthenticationProvider.EVERYONE_NAME;
        }

    };

    /**
     * User role for Fedora's admin users
     */
    public static final String FEDORA_ADMIN_ROLE = "fedoraAdmin";

    /**
     * User role for Fedora's ordinary users
     */
    public static final String FEDORA_USER_ROLE = "fedoraUser";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServletContainerAuthenticationProvider.class);

    private Set<PrincipalProvider> principalProviders = Collections.emptySet();

    private FedoraAuthorizationDelegate fad;

    /**
     * Provides the singleton bean to ModeShape via reflection based on class
     * name.
     *
     * @return a AuthenticationProvider
     */
    public static synchronized AuthenticationProvider getInstance() {
        if (_instance != null) {
            return _instance;
        }
        _instance = new ServletContainerAuthenticationProvider();
        LOGGER.warn("Security is MINIMAL, no Policy Enforcement Point configured.");
        return _instance;
    }

    /**
     * @return the principalProviders
     */
    public Set<PrincipalProvider> getPrincipalProviders() {
        return principalProviders;
    }

    /**
     * @param principalProviders the principalProviders to set
     */
    public void setPrincipalProviders(
            final Set<PrincipalProvider> principalProviders) {
        this.principalProviders = principalProviders;
    }

    /**
     * Authenticate the user that is using the supplied credentials.
     * <p>
     * If the credentials given establish that the authenticated user has the
     * fedoraAdmin role, construct an ExecutionContext with
     * FedoraAdminSecurityContext as the SecurityContext. Otherwise, construct
     * an ExecutionContext with FedoraUserSecurityContext as the
     * SecurityContext.
     * </p>
     * <p>
     * If user has the fedoraUser role, add additional information to the
     * session attributes so that authorization can be performed by the
     * authorization delegate. Currently, this includes the servlet request, a
     * principal instance representing the authenticated user, and a set of all
     * principals representing the credentials given (e.g. the authenticated
     * user and their groups).
     * </p>
     * <p>
     * If the user has neither the fedoraUser or fedoraAdmin role, add only the
     * singleton set containing the EVERYONE principal to the session
     * attributes.
     * </p>
     */
    @Override
    public ExecutionContext authenticate(final Credentials credentials,
            final String repositoryName, final String workspaceName,
            final ExecutionContext repositoryContext,
            final Map<String, Object> sessionAttributes) {
        LOGGER.debug("in authenticate: {}; FAD: {}", credentials, fad);

        if (!(credentials instanceof ServletCredentials)) {
            return null;
        }

        final HttpServletRequest servletRequest =
                ((ServletCredentials) credentials).getRequest();
        final Principal userPrincipal = servletRequest.getUserPrincipal();

        if (userPrincipal != null &&
                servletRequest.isUserInRole(FEDORA_ADMIN_ROLE)) {
            return repositoryContext.with(new FedoraAdminSecurityContext(
                    userPrincipal.getName()));
        }

        if (userPrincipal != null &&
                servletRequest.isUserInRole(FEDORA_USER_ROLE)) {

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_SERVLET_REQUEST,
                    servletRequest);

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_USER_PRINCIPAL,
                    userPrincipal);

            final Set<Principal> principals = collectPrincipals(credentials);
            principals.add(userPrincipal);
            principals.add(EVERYONE);

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_ALL_PRINCIPALS,
                    principals);

        } else {

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_ALL_PRINCIPALS,
                    Collections.singleton(EVERYONE));

        }

        return repositoryContext.with(new FedoraUserSecurityContext(
                userPrincipal, fad));
    }

    /**
     * @return the authorization delegate
     */
    public FedoraAuthorizationDelegate getFad() {
        return fad;
    }

    /**
     * @param fad the authorization delegate to set
     */
    public void setFad(final FedoraAuthorizationDelegate fad) {
        this.fad = fad;
    }

    private Set<Principal> collectPrincipals(final Credentials credentials) {
        final HashSet<Principal> principals = new HashSet<>();

        // TODO add exception handling for principal providers
        for (final PrincipalProvider p : this.getPrincipalProviders()) {
            final Set<Principal> ps = p.getPrincipals(credentials);

            if (ps != null) {
                principals.addAll(p.getPrincipals(credentials));
            }
        }

        return principals;
    }
}
