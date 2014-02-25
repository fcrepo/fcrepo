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

    private Set<HTTPPrincipalFactory> principalFactories = Collections
            .emptySet();

    private FedoraPolicyEnforcementPoint pep;

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
     * @return the principalFactories
     */
    public Set<HTTPPrincipalFactory> getPrincipalFactories() {
        return principalFactories;
    }

    /**
     * @param principalFactories the principalFactories to set
     */
    public void setPrincipalFactories(
            final Set<HTTPPrincipalFactory> principalFactories) {
        this.principalFactories = principalFactories;
    }

    /**
     * @see org.modeshape.jcr.security.AuthenticationProvider
     *      #authenticate(javax.jcr.Credentials, java.lang.String,
     *      java.lang.String, org.modeshape.jcr.ExecutionContext, java.util.Map)
     */
    @Override
    public ExecutionContext authenticate(final Credentials credentials,
            final String repositoryName, final String workspaceName,
            final ExecutionContext repositoryContext,
            final Map<String, Object> sessionAttributes) {
        LOGGER.debug("in authenticate: {}; PEP: {}", credentials, pep);

        if (!(credentials instanceof ServletCredentials)) {
            return null;
        }

        final ServletCredentials creds = (ServletCredentials) credentials;

        // does this request have the fedoraAdmin role in the container?
        if (creds.getRequest().getUserPrincipal() != null &&
                creds.getRequest().isUserInRole(FEDORA_ADMIN_ROLE)) {
            return repositoryContext.with(new FedoraAdminSecurityContext(creds
                    .getRequest().getUserPrincipal().getName()));
        }

        // add base public principals
        final Set<Principal> principals = new HashSet<>();
        principals.add(EVERYONE); // all sessions have this principal

        // request fedora user role to add user principal
        if (creds.getRequest().getUserPrincipal() != null &&
                creds.getRequest().isUserInRole(FEDORA_USER_ROLE)) {
            principals.add(creds.getRequest().getUserPrincipal());
            // get user details/principals
            addUserPrincipals(creds.getRequest(), principals);
        }
        return repositoryContext.with(new FedoraUserSecurityContext(creds,
                principals, pep));
    }

    /**
     * @return the pep
     */
    public FedoraPolicyEnforcementPoint getPep() {
        return pep;
    }

    /**
     * @param pep the pep to set
     */
    public void setPep(final FedoraPolicyEnforcementPoint pep) {
        this.pep = pep;
    }

    private void addUserPrincipals(final HttpServletRequest request,
            final Set<Principal> principals) {
        // TODO add exception handling for principal factories
        for (final HTTPPrincipalFactory pf : this.getPrincipalFactories()) {
            principals.addAll(pf.getGroupPrincipals(request));
        }
    }
}
