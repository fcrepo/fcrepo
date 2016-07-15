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

import static org.modeshape.jcr.ModeShapePermissions.READ;
import static org.modeshape.jcr.ModeShapePermissions.REGISTER_NAMESPACE;
import static org.modeshape.jcr.ModeShapePermissions.REGISTER_TYPE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.security.Principal;
import java.util.Set;

import org.modeshape.jcr.security.AdvancedAuthorizationProvider;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

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

        if (absPath == null) {
            // this permission is required for login
            if (actions.length == 1 && READ.equals(actions[0])) {
                return true;
            }
            // The REGISTER_NAMESPACE action and the REGISTER_TYPE action don't include
            // a path and are allowed for all users.  The fedora 4 code base doesn't expose
            // any endpoint that *JUST* registers a namespace or type, so the operations
            // that perform these actions will have to be authorized in context (for instance
            // setting a property).
            final Set<String> filteredActions = Sets.newHashSet(actions);
            filteredActions.remove(REGISTER_NAMESPACE);
            filteredActions.remove(REGISTER_TYPE);
            return filteredActions.isEmpty();
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
