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
package org.fcrepo.auth.webac;

import static java.util.stream.Collectors.toSet;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.auth.common.FedoraAuthorizationDelegate;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Policy enforcement point for roles-based authentication
 * @author Gregory Jansen
 */
public abstract class AbstractRolesAuthorizationDelegate implements FedoraAuthorizationDelegate {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractRolesAuthorizationDelegate.class);

    protected static final String AUTHZ_DETECTION = "/{" +
            "http://fedora.info/definitions/v4/authorization#" + "}";

    private static final String[] REMOVE_ACTIONS = {"remove"};

    @Inject
    private AccessRolesProvider accessRolesProvider = null;

    @Inject
    private SessionFactory sessionFactory = null;

    /**
     * Gather effectives roles
     *
     * @param acl access control list
     * @param principals effective principals of agents and foaf agentClass
     * @return set of effective content roles
     */
    public static Set<String> resolveUserRoles(final Map<String, Collection<String>> acl,
                    final Collection<Principal> principals) {
        return principals.stream().map(Principal::getName).filter(acl::containsKey)
            .peek(principal -> LOGGER.debug("request principal matched role assignment: {}", principal))
            .map(acl::get)
            .flatMap(Collection::stream)
            .collect(toSet());
    }

    @Override
    public boolean hasPermission(final Session session, final Path absPath, final String[] actions) {
        LOGGER.debug("Does user have permission for actions: {}, on path: {}", actions, absPath);
        final boolean permission = doHasPermission(session, absPath, actions);

        LOGGER.debug("Permission for actions: {}, on: {} = {}", actions, absPath, permission);
        return permission;
    }

    private boolean doHasPermission(final Session session, final Path absPath, final String[] actions) {
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
            final FedoraSession internalSession = sessionFactory.getInternalSession();
            final Map<String, Collection<String>> acl =
                    accessRolesProvider.findRolesForPath(absPath,
                            getJcrSession(internalSession));
            roles = resolveUserRoles(acl, allPrincipals);
            LOGGER.debug("roles for this request: {}", roles);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException("Cannot look up node information on " + absPath +
                    " for permissions check.", e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("roles: {}, actions: {}, path: {}", roles, actions, absPath);
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

        if (!rolesHavePermission(session, absPath.toString(), actions, roles)) {
            return false;
        }

        if (actions.length == 1 && "remove".equals(actions[0])) {
            // you must be able to delete all the children
            // TODO make recursive/ACL-query-based check configurable
            return canRemoveChildrenRecursive(session, absPath.toString(),
                    allPrincipals, roles);
        }
        return true;
    }

    private static Principal getUserPrincipal(final Session session) {
        final Object value = session.getAttribute(FEDORA_USER_PRINCIPAL);
        if (value instanceof Principal) {
            return (Principal) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<Principal> getPrincipals(final Session session) {
        final Object value = session.getAttribute(FEDORA_ALL_PRINCIPALS);
        if (value instanceof Set<?>) {
            return (Set<Principal>) value;
        }
        return null;
    }

    /**
     * @param userSession the user session
     * @param parentPath the parent path
     * @param allPrincipals all principals
     * @param parentRoles the roles on the parent
     * @return true if permitted
     */
    private boolean canRemoveChildrenRecursive(final Session userSession,
                                               final String parentPath,
                                               final Set<Principal> allPrincipals,
                                               final Set<String> parentRoles) {
        try {
            final FedoraSession internalSession = sessionFactory.getInternalSession();
            LOGGER.debug("Recursive child remove permission checks for: {}",
                    parentPath);
            final Item item = getJcrSession(internalSession).getItem(parentPath);
            if (!item.isNode()) {
                // this is a property and has no children...
                return true;
            }
            final Node parent = (Node) item;
            if (!parent.hasNodes()) {
                return true;
            }
            final NodeIterator ni = parent.getNodes();
            while (ni.hasNext()) {
                final Node n = ni.nextNode();
                // are there unique roles?
                final Set<String> roles;
                final Map<String, Collection<String>> acl = accessRolesProvider.getRoles(n, false);

                if (acl != null) {
                    roles = resolveUserRoles(acl, allPrincipals);
                } else {
                    roles = parentRoles;
                }
                if (rolesHavePermission(userSession, n.getPath(),
                        REMOVE_ACTIONS,
                        roles)) {

                    if (!canRemoveChildrenRecursive(userSession, n.getPath(),
                            allPrincipals, roles)) {
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
     * @param userSession the user session
     * @param absPath path to the object
     * @param actions requested action
     * @param roles effective roles for this request and content
     * @return true if role has permission
     */
    public abstract boolean rolesHavePermission(final Session userSession, final String absPath,
            final String[] actions, final Set<String> roles);

}