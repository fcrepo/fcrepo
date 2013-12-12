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

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.auth.common.FedoraPolicyEnforcementPoint;
import org.fcrepo.http.commons.session.SessionFactory;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Gregory Jansen
 */
public abstract class AbstractRolesPEP implements FedoraPolicyEnforcementPoint {

    private static final Logger log = LoggerFactory
            .getLogger(AbstractRolesPEP.class);

    protected static final String AUTHZ_DETECTION = "/{" +
            Constants.JcrName.NS_URI + "}";

    private static final String[] READ_ACTIONS = {"read"};

    private static final String[] REMOVE_ACTIONS = {"remove"};

    @Autowired
    private AccessRolesProvider accessRolesProvider = null;

    /**
     * @return the accessRolesProvider
     */
    public AccessRolesProvider getAccessRolesProvider() {
        return accessRolesProvider;
    }

    /**
     * @param accessRolesProvider the accessRolesProvider to set
     */
    public void setAccessRolesProvider(
            final AccessRolesProvider accessRolesProvider) {
        this.accessRolesProvider = accessRolesProvider;
    }

    @Autowired
    private SessionFactory sessionFactory = null;

    /**
     * @return the sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * @param sessionFactory the sessionFactory to set
     */
    public void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.auth.FedoraPolicyEnforcementPoint#filterPathsForReading(java
     * .util.Collection, java.util.Set, java.security.Principal)
     */
    @Override
    public Iterator<Path> filterPathsForReading(final Iterator<Path> paths,
            final Set<Principal> allPrincipals, final Principal userPrincipal) {
        Session session = null;
        try {
            session = sessionFactory.getInternalSession();
        } catch (final RepositoryException e) {
            throw new Error("PEP cannot obtain an internal session", e);
        }
        return new PathIterator(session, paths,
                userPrincipal, allPrincipals);
    }

    /**
     * Gather effectives roles
     *
     * @param principals effective principals
     * @return set of effective content roles
     * @throws RepositoryException if role discovery fails
     */
    public static Set<String>
    resolveUserRoles(final Map<String, List<String>> acl,
                    final Set<Principal> principals) throws RepositoryException {
        final Set<String> roles = new HashSet<String>();
        for (final Principal p : principals) {
            final List<String> matchedRoles = acl.get(p.getName());
            if (matchedRoles != null) {
                log.debug("request principal matched role assignment: {}", p
                        .getName());
                roles.addAll(matchedRoles);
            }
        }
        return roles;
    }

    @Override
    public boolean hasModeShapePermission(final Path absPath,
            final String[] actions, final Set<Principal> allPrincipals,
            final Principal userPrincipal) {
        final boolean newNode = false;
        Set<String> roles = null;
        JcrSession session = null;
        try {
            session = (JcrSession)sessionFactory.getInternalSession();
            final Map<String, List<String>> acl =
                    accessRolesProvider.findRolesForPath(absPath, session);
            roles = resolveUserRoles(acl, allPrincipals);
            log.debug("roles for this request: {}", roles);
        } catch (final RepositoryException e) {
            throw new Error("Cannot look up node information on " + absPath +
                    " for permissions check.", e);
        }

        if (log.isDebugEnabled()) {
            final StringBuilder msg = new StringBuilder();
            msg.append(roles.toString()).append("\t").append(
                    Arrays.toString(actions)).append("\t").append(
                    newNode ? "NEW" : "OLD").append("\t").append(
                    (absPath == null ? absPath : absPath.toString()));
            log.debug(msg.toString());
            if (actions.length > 1) { // have yet to see more than one
                log.debug("FOUND MULTIPLE ACTIONS: {}", Arrays
                        .toString(actions));
            }
        }
        if (actions.length == 1 && "remove_child_nodes".equals(actions[0])) {
            // in roles-based ACLs, the permission to remove children is
            // conferred by earlier check for "remove_node" on the child node
            // itself.
            return true;
        }
        if (!rolesHaveModeShapePermission(absPath.toString(), actions,
                allPrincipals,
                userPrincipal, roles)) {
            return false;
        }
        if (actions.length == 1 && "remove".equals(actions[0])) {
            // you must be able to delete all the children
            // TODO make recursive/ACL-query-based check configurable
            return canRemoveChildrenRecursive(absPath.toString(), session,
                    allPrincipals,
                    userPrincipal, roles);
        } else {
            return true;
        }
    }

    /**
     * @param absPath
     * @param actions
     * @param allPrincipals
     * @param userPrincipal
     * @return
     */
    private boolean canRemoveChildrenRecursive(final String parentPath,
            final JcrSession session, final Set<Principal> allPrincipals,
            final Principal userPrincipal, final Set<String> parentRoles) {
        try {
            log.debug("Recursive child remove permission checks for: {}",
                    parentPath);
            final Node parent = session.getNode(parentPath);
            if (!parent.hasNodes()) {
                return true;
            }
            final NodeIterator ni = parent.getNodes();
            while (ni.hasNext()) {
                final Node n = ni.nextNode();
                // are there unique roles?
                Set<String> roles = null;
                Map<String, List<String>> acl = null;
                try {
                    acl = accessRolesProvider.getRoles(n, false);
                } catch (final PathNotFoundException ignored) {
                }
                if (acl != null) {
                    roles = resolveUserRoles(acl, allPrincipals);
                } else {
                    roles = parentRoles;
                }
                if (rolesHaveModeShapePermission(n.getPath(), REMOVE_ACTIONS,
                        allPrincipals, userPrincipal, roles)) {

                    if (!canRemoveChildrenRecursive(n.getPath(), session,
                            allPrincipals, userPrincipal, roles)) {
                        return false;
                    }
                } else {
                    log.info("Remove permission denied at {} with roles {}", n
                            .getPath(), roles);
                    return false;
                }
            }
            return true;
        } catch (final RepositoryException e) {
            throw new Error(
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
     * @param userPrincipal
     * @param allPrincipals
     * @param roles effective roles for this request and content
     * @return true if role has permission
     */
    public abstract boolean rolesHaveModeShapePermission(String absPath,
            String[] actions, Set<Principal> allPrincipals,
            Principal userPrincipal, Set<String> roles);

    public class PathIterator implements Iterator<Path> {

        private Path next = null;

        private Session session = null;

        private Iterator<Path> wrapped = null;

        private Principal userPrincipal = null;

        private Set<Principal> principals = null;

        /**
         * @param session
         * @param paths
         * @param allPrincipals
         */
        public PathIterator(final Session session,
                final Iterator<Path> paths, final Principal userPrincipal,
                final Set<Principal> allPrincipals) {
            this.wrapped = paths;
            this.session = session;
            this.userPrincipal = userPrincipal;
            this.principals = allPrincipals;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                findNext();
            }
            return next != null;
        }

        @Override
        public Path next() {
            if (next == null) {
                findNext();
            }
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "This API is for reads only");
        }

        private void findNext() {
            while (wrapped.hasNext()) {
                final Path p = wrapped.next();
                // lookup roles
                try {
                    final Map<String, List<String>> acl =
                            accessRolesProvider.findRolesForPath(p, session);
                    final Set<String> roles = resolveUserRoles(acl, principals);
                    if (rolesHaveModeShapePermission(p.getString(),
                            READ_ACTIONS,
                            principals, userPrincipal, roles)) {
                        next = p;
                        break;
                    }
                } catch (final RepositoryException e) {
                    throw new Error("Cannot look up node information on " + p +
                            " for permissions check.", e);
                }
            }
        }
    }

}
