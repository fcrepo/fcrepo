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

import static org.fcrepo.kernel.api.FedoraJcrTypes.JCR_CONTENT;

import java.security.Principal;

import org.modeshape.jcr.security.AdvancedAuthorizationProvider;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The security context for Fedora servlet users. These users are not
 * necessarily authenticated by the container, i.e. users may include the
 * general public. This security context delegates all access decisions to the
 * configured authorization delegate.
 *
 * @author Gregory Jansen
 */
public class FedoraUserSecurityContext implements SecurityContext,
        AdvancedAuthorizationProvider {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FedoraUserSecurityContext.class);

    private Principal userPrincipal = null;

    private FedoraAuthorizationDelegate fad = null;

    private boolean loggedIn = true;

    /**
     * Constructs a new security context.
     *
     * @param userPrincipal the user principal associated with this security
     *        context
     * @param fad the authorization delegate
     */
    protected FedoraUserSecurityContext(final Principal userPrincipal,
            final FedoraAuthorizationDelegate fad) {
        this.fad = fad;
        this.userPrincipal = userPrincipal;

        if (this.fad == null) {
            LOGGER.warn("This security context must have a FAD injected");
            throw new IllegalArgumentException(
                    "This security context must have a FAD injected");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.jcr.security.SecurityContext#isAnonymous()
     */
    @Override
    public boolean isAnonymous() {
        return this.userPrincipal == null;
    }

    /**
     * {@inheritDoc}
     *
     * @see SecurityContext#getUserName()
     */
    @Override
    public final String getUserName() {
        return getEffectiveUserPrincipal().getName();
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
        if ("read".equals(roleName)) {
            return true;
        } else if ("write".equals(roleName)) {
            return true;
        } else if ("admin".equals(roleName)) {
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

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.jcr.security.SecurityContext#logout()
     */
    @Override
    public void logout() {
        this.loggedIn = false;
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
        if (!this.loggedIn) {
            return false;
        }

        // this permission is required for login
        if (absPath == null) {
            return actions.length == 1 && "read".equals(actions[0]);
        }

        // Trim jcr:content from paths, if necessary
        final Path path;
        if (null != absPath.getLastSegment() && absPath.getLastSegment().getString().equals(JCR_CONTENT)) {
            path = absPath.subpath(0, absPath.size() - 1);
            LOGGER.debug("..new path to be verified: {}", path);
        } else {
            path = absPath;
        }

        // delegate
        if (fad != null) {
            return fad.hasPermission(context.getSession(), path, actions);
        }
        return false;
    }
}
