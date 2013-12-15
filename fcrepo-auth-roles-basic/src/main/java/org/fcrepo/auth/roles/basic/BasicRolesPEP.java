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

package org.fcrepo.auth.roles.basic;

import java.security.Principal;
import java.util.Set;

import org.fcrepo.auth.roles.common.AbstractRolesPEP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gregory Jansen
 */
public class BasicRolesPEP extends AbstractRolesPEP {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BasicRolesPEP.class);

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.roles.AbstractRolesPEP#hasModeShapePermission(org.modeshape
     * .jcr.value.Path, java.lang.String[], java.util.Set,
     * java.security.Principal, java.util.Set)
     */
    @Override
    public boolean rolesHaveModeShapePermission(final String absPath,
            final String[] actions, final Set<Principal> allPrincipals,
            final Principal userPrincipal, final Set<String> roles) {
        if (roles.isEmpty()) {
            LOGGER.debug("A caller without content roles can do nothing in the repository.");
            return false;
        }
        if (roles.contains("admin")) {
            LOGGER.debug("Granting an admin role permission to perform any action.");
            return true;
        }
        if (roles.contains("writer")) {
            if (absPath.contains(AUTHZ_DETECTION)) {
                LOGGER.debug("Denying writer role permission to perform an action on an ACL node.");
                return false;
            } else {
                LOGGER.debug("Granting writer role permission to perform any action on a non-ACL nodes.");
                return true;
            }
        }
        if (roles.contains("reader")) {
            if (actions.length == 1 && "read".equals(actions[0])) {
                LOGGER.debug("Granting reader role permission to perform a read action.");
                return true;
            } else {
                LOGGER.debug("Denying reader role permission to perform a non-read action.");
                return false;
            }
        }
        LOGGER.error("There are roles in session that aren't recognized by this PEP: {}",
                     roles);
        return false;
    }

}
