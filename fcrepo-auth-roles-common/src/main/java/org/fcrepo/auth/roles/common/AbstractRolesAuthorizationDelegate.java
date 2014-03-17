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

package org.fcrepo.auth.roles.common;

import org.fcrepo.auth.common.FedoraAuthorizationDelegate;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Policy enforcement point for roles-based authentication
 * @author Gregory Jansen
 */
public abstract class AbstractRolesAuthorizationDelegate implements FedoraAuthorizationDelegate {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractRolesAuthorizationDelegate.class);

    protected static final String AUTHZ_DETECTION = "/{" +
            Constants.JcrName.NS_URI + "}";

    private static final String[] REMOVE_ACTIONS = {"remove"};

    @Autowired
    private AccessRolesProvider accessRolesProvider = null;

    @Autowired
    private SessionFactory sessionFactory = null;

    /**
     * Gather effectives roles
     *
     * @param principals effective principals
     * @return set of effective content roles
     */
    public static Set<String>
    resolveUserRoles(final Map<String, List<String>> acl,
                    final Set<Principal> principals) {
        final Set<String> roles = new HashSet<>();
        for (final Principal p : principals) {
            final List<String> matchedRoles = acl.get(p.getName());
            if (matchedRoles != null) {
                LOGGER.debug("request principal matched role assignment: {}", p.getName());
                roles.addAll(matchedRoles);
            }
        }
        return roles;
    }

    @Override
    public boolean hasPermission(Session session, final Path absPath,
            final String[] actions) {
        final Set<String> roles;

        final Principal userPrincipal = getUserPrincipal(session);
        if (userPrincipal == null) {
            return false;
        }

        final Set<Principal> allPrincipals = getPrincipals(session);
        if (allPrincipals == null) {
            return false;
        }

        try {
            final Session internalSession = sessionFactory.getInternalSession();
            final Map<String, List<String>> acl =
                    accessRolesProvider.findRolesForPath(absPath,
                            internalSession);
            roles = resolveUserRoles(acl, allPrincipals);
            LOGGER.debug("roles for this request: {}", roles);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot look up node information on " + absPath +
                    " for permissions check.", e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}\t{}\t{}", roles, actions, absPath);
            if (actions.length > 1) { // have yet to see more than one
                LOGGER.debug("FOUND MULTIPLE ACTIONS: {}", Arrays
                        .toString(actions));
            }
        }

        if (actions.length == 1 && "remove_child_nodes".equals(actions[0])) {
            // in roles-based ACLs, the permission to remove children is
            // conferred by earlier check for "remove_node" on the child node
            // itself.
            return true;
        }

        if (!rolesHavePermission(absPath.toString(), actions, roles)) {
            return false;
        }

        if (actions.length == 1 && "remove".equals(actions[0])) {
            // you must be able to delete all the children
            // TODO make recursive/ACL-query-based check configurable
            return canRemoveChildrenRecursive(absPath.toString(), allPrincipals, roles);
        }
        return true;
    }

    private Principal getUserPrincipal(Session session) {
        final Object value = session.getAttribute(FEDORA_USER_PRINCIPAL);
        if (value instanceof Principal) {
            return (Principal) value;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Principal> getPrincipals(Session session) {
        final Object value = session.getAttribute(FEDORA_ALL_PRINCIPALS);
        if (value instanceof Set<?>) {
            return (Set<Principal>) value;
        } else {
            return null;
        }
    }

    /**
     * @param parentPath
     * @param allPrincipals
     * @param parentRoles
     * @return
     */
    private boolean canRemoveChildrenRecursive(final String parentPath,
                                               final Set<Principal> allPrincipals,
                                               final Set<String> parentRoles) {
        try {
            final Session internalSession = sessionFactory.getInternalSession();
            LOGGER.debug("Recursive child remove permission checks for: {}",
                         parentPath);
            final Node parent = internalSession.getNode(parentPath);
            if (!parent.hasNodes()) {
                return true;
            }
            final NodeIterator ni = parent.getNodes();
            while (ni.hasNext()) {
                final Node n = ni.nextNode();
                // are there unique roles?
                final Set<String> roles;
                Map<String, List<String>> acl = null;
                try {
                    acl = accessRolesProvider.getRoles(n, false);
                } catch (final PathNotFoundException ignored) {
                    LOGGER.trace("Path not found when removing roles", ignored);
                }
                if (acl != null) {
                    roles = resolveUserRoles(acl, allPrincipals);
                } else {
                    roles = parentRoles;
                }
                if (rolesHavePermission(n.getPath(), REMOVE_ACTIONS,
                        roles)) {

                    if (!canRemoveChildrenRecursive(n.getPath(), allPrincipals, roles)) {
                        return false;
                    }
                } else {
                    LOGGER.info("Remove permission denied at {} with roles {}", n.getPath(), roles);
                    return false;
                }
            }
            return true;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Cannot lookup child permission check information for " +
                            parentPath, e);
        }
    }

    /**
     * Subclasses must override this method to determine permissions based on
     * supplied roles.
     *
     * @param absPath path to the object
     * @param actions requested action
     * @param roles effective roles for this request and content
     * @return true if role has permission
     */
    public abstract boolean rolesHavePermission(final String absPath,
            final String[] actions, final Set<String> roles);

}
