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

package org.fcrepo.auth;

import java.security.Principal;
import java.util.Arrays;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.modeshape.jcr.security.AdvancedAuthorizationProvider;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The security context for Fedora servlet users. These users are not
 * necessarily authenticated by the container, i.e. users may include the
 * general public. This security context delegates all access decisions to the
 * configured Fedora policy enforcement point.
 * 
 * @author Gregory Jansen
 */
public class FedoraUserSecurityContext implements SecurityContext,
        AdvancedAuthorizationProvider {

    private static Logger logger = LoggerFactory
            .getLogger(FedoraUserSecurityContext.class);

    private Set<Principal> principals = null;

    private HttpServletRequest request = null;

    private FedoraPolicyEnforcementPoint pep = null;

    /**
     * Constructs a new security context.
     * 
     * @param request the servlet request
     * @param principals security principals associated with this request
     * @param pep the policy enforcement point
     */
    protected FedoraUserSecurityContext(final HttpServletRequest request,
            final Set<Principal> principals,
            final FedoraPolicyEnforcementPoint pep) {
        this.request = request;
        this.principals = principals;
        this.pep = pep;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.security.SecurityContext#isAnonymous()
     */
    @Override
    public boolean isAnonymous() {
        return false;
    }

    /**
     * {@inheritDoc SecurityContext#getUserName()}
     * 
     * @see SecurityContext#getUserName()
     */
    @Override
    public final String getUserName() {
        return request.getRemoteUser();
    }

    /**
     * {@inheritDoc SecurityContext#hasRole(String)}
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
        // return request != null && request.isUserInRole(roleName);
    }

    /**
     * Get the user principal associated with this context.
     * 
     * @return
     */
    public Principal getUserPrincipal() {
        return this.request.getUserPrincipal();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.security.SecurityContext#logout()
     */
    @Override
    public void logout() {
        request = null;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.modeshape.jcr.security.AdvancedAuthorizationProvider#hasPermission
     * (org.modeshape.jcr.security.AdvancedAuthorizationProvider.Context,
     * org.modeshape.jcr.value.Path, java.lang.String[]) grabs AuthZ attributes
     * from context and delegates to a handler class subject: user roles
     * (abstract privileges?) group principals (on campus) (URIs? attrIds?) the
     * user principle resource attributes: mix-in types JCR path environment
     * attributes: ??
     */
    @Override
    public boolean hasPermission(final Context context,
            final Path absPath, final String... actions) {
        logger.debug("in hasPermission");
        // what roles do these principals have in repo (MODE-1920)
        // final Privilege[] privs =
        // context.getSession().getAccessControlManager()
        // .getPrivileges(absPath.toString());
        // final AccessControlPolicy[] policies =
        // context.getSession().getAccessControlManager()
        // .getEffectivePolicies(absPath.toString());
        // policies[0].getClass();

        logger.debug("hasPermission(" + context + "," +
                (absPath == null ? absPath : absPath.getString()) + "," +
                Arrays.toString(actions) + ")");

        // this permission is required for login
        if (absPath == null) {
            return actions.length == 1 && "read".equals(actions[0]);
        }

        // delegate to Fedora PDP
        if (pep != null) {
            return pep.hasModeShapePermission(absPath, actions,
                    this.principals, getUserPrincipal());
        } else {
            return true;
        }
    }
}