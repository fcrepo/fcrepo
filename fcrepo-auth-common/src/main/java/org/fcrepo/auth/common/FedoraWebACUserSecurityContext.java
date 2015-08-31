/**
 * Copyright 2015 DuraSpace, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The security context for Fedora WebAC servlet users. These users are not
 * necessarily authenticated by the container, i.e. users may include the
 * general public. This security context delegates all access decisions to the
 * configured authorization delegate.
 *
 * @author mohideen
 */
public class FedoraWebACUserSecurityContext extends FedoraUserSecurityContext {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FedoraWebACUserSecurityContext.class);

    /**
     * Constructs a new security context.
     *
     * @param userPrincipal the user principal associated with this security
     *        context
     * @param fad the authorization delegate
     */
    protected FedoraWebACUserSecurityContext(final Principal userPrincipal,
                                        final FedoraAuthorizationDelegate fad) {
        super(userPrincipal, fad);
    }

    /**
     * {@inheritDoc}
     *
     * @see SecurityContext#hasRole(String)
     */
    @Override
    public final boolean hasRole(final String roleName) {
        // Under this custom PEP regime, all users have modeshape read and write
        // roles.
        if ("http://www.w3.org/ns/auth/acl#Read".equals(roleName)) {
            return true;
        } else if ("http://www.w3.org/ns/auth/acl#Write".equals(roleName)) {
            return true;
        } else if ("http://www.w3.org/ns/auth/acl#Append".equals(roleName)) {
            return true;
        } else if ("http://www.w3.org/ns/auth/acl#Control".equals(roleName)) {
            return true;
        }
        return false;
    }

    /**
     * Get the user principal associated with this context.
     *
     * @return the user principal associated with this security context
     */
    public Principal getEffectiveUserPrincipal() {
        if (this.loggedIn && this.userPrincipal != null) {
            return this.userPrincipal;
        }
        return fad.getEveryonePrincipal();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.modeshape.jcr.security.AdvancedAuthorizationProvider#hasPermission
     * (org.modeshape.jcr.security.AdvancedAuthorizationProvider.Context,
     * org.modeshape.jcr.value.Path, java.lang.String[])
     */
    @Override
    public boolean hasPermission(final Context context, final Path absPath,
                                 final String... actions) {

        LOGGER.debug("Verifying hasPermission on path: " + absPath + " for: " + StringUtils.join(actions));

        if (!this.loggedIn) {
            return false;
        }

        // this permission is required for login
        if (absPath == null) {
            return actions.length == 1 && "read".equals(actions[0]);
        }

        // delegate
        if (fad != null) {
            return fad.hasPermission(context.getSession(), absPath, actions);
        }
        return false;
    }
}
