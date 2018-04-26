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

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.servlet.http.HttpServletRequest;

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.ServletCredentials;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authenticates ModeShape logins where JAX-RS credentials are supplied. Capable
 * of authenticating whether or not container has performed user authentication.
 * This is a singleton with an injected policy enforcement point. The singleton
 * pattern allows ModeShape to obtain this instance via classname configuration.
 *
 * @author Gregory Jansen
 */
public final class ServletContainerAuthenticationProvider implements
        AuthenticationProvider {

    private static ServletContainerAuthenticationProvider instance = null;

    private ServletContainerAuthenticationProvider() {
        instance = this;
    }

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
        if (instance != null) {
            return instance;
        }
        instance = new ServletContainerAuthenticationProvider();
        LOGGER.warn("Security is MINIMAL, no Policy Enforcement Point configured.");
        return instance;
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
     * If the credentials given establish that the authenticated user has the fedoraAdmin role, construct an
     * ExecutionContext with FedoraAdminSecurityContext as the SecurityContext. Otherwise, construct an
     * ExecutionContext with FedoraUserSecurityContext as the SecurityContext.
     * </p>
     * <p>
     * If the authenticated user does not have the fedoraAdmin role, session attributes will be assigned in the
     * sessionAttributes map:
     * </p>
     * <ul>
     * <li>FEDORA_SERVLET_REQUEST will be assigned the ServletRequest instance associated with credentials.</li>
     * <li>FEDORA_ALL_PRINCIPALS will be assigned the union of all principals obtained from configured
     * PrincipalProvider instances plus the authenticated user's principal; FEDORA_ALL_PRINCIPALS will be assigned the
     * singleton set containing the fad.getEveryonePrincipal() principal otherwise.</li>
     * </ul>
     */
    @Override
    public ExecutionContext authenticate(final Credentials credentials,
            final String repositoryName, final String workspaceName,
            final ExecutionContext repositoryContext,
            final Map<String, Object> sessionAttributes) {
        LOGGER.debug("Trying to authenticate: {}; FAD: {}", credentials, fad);

        if (!(credentials instanceof ServletCredentials)) {
            return null;
        }

        final HttpServletRequest servletRequest =
                ((ServletCredentials) credentials).getRequest();
        Principal userPrincipal = servletRequest.getUserPrincipal();

        if (userPrincipal != null && servletRequest.isUserInRole(FEDORA_ADMIN_ROLE)) {
            // check if delegation is configured
            final Principal delegatedPrincipal = getDelegatedPrincipal(credentials);
            if (delegatedPrincipal != null) {
                // replace the userPrincipal with the delegated principal
                // then fall through to the normal user processing
                userPrincipal = delegatedPrincipal;
                LOGGER.info("Admin user is delegating to {}", userPrincipal);

            } else {
                // delegation is configured, but there is no delegated user set in the header of this request
                LOGGER.debug("Returning admin user");
                return repositoryContext.with(new FedoraAdminSecurityContext(userPrincipal.getName()));
            }
        }

        if (userPrincipal != null) {
            LOGGER.debug("Found user-principal: {}.", userPrincipal.getName());

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_SERVLET_REQUEST,
                    servletRequest);

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_USER_PRINCIPAL,
                    userPrincipal);

            final Set<Principal> principals = collectPrincipals(credentials);
            principals.add(userPrincipal);
            principals.add(fad.getEveryonePrincipal());

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_ALL_PRINCIPALS,
                    principals);

            LOGGER.debug("All principals: {}", principals);

        } else {
            LOGGER.debug("No user-principal found.");

            sessionAttributes.put(FedoraAuthorizationDelegate.FEDORA_USER_PRINCIPAL,
                    fad.getEveryonePrincipal());

            sessionAttributes.put(
                    FedoraAuthorizationDelegate.FEDORA_ALL_PRINCIPALS,
                    Collections.singleton(fad.getEveryonePrincipal()));

        }

        return repositoryContext.with(new FedoraUserSecurityContext(
                userPrincipal, fad));
    }

    private Principal getDelegatedPrincipal(final Credentials credentials) {
        for (final PrincipalProvider provider : this.getPrincipalProviders()) {
            if (provider instanceof DelegateHeaderPrincipalProvider) {
                final HttpServletRequest request = ((ServletCredentials) credentials).getRequest();
                return ((DelegateHeaderPrincipalProvider) provider).getDelegate(request);
            }
        }
        return null;
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
        final Set<Principal> principals = new HashSet<>();

        // TODO add exception handling for principal providers
        for (final PrincipalProvider p : this.getPrincipalProviders()) {
            // if the provider is DelegateHeader, it is either already processed (if logged user has fedora admin role)
            // or should be ignored completely (the user was not in admin role, so on-behalf-of header must be ignored)
            if (!(p instanceof DelegateHeaderPrincipalProvider)) {
                final Set<Principal> ps = p.getPrincipals(((ServletCredentials) credentials).getRequest());

                if (ps != null) {
                    principals.addAll(p.getPrincipals(((ServletCredentials) credentials).getRequest()));
                }
            }
        }

        return principals;
    }
}
