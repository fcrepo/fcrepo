/**
 * Copyright 2014 DuraSpace, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.auth.roles.common.Constants.JcrName;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static com.google.common.collect.Iterables.toArray;
import static java.util.Collections.emptyMap;
import static org.fcrepo.auth.roles.common.Constants.registerPrefixes;
import static org.fcrepo.auth.roles.common.Constants.JcrName.Assignment;
import static org.fcrepo.auth.roles.common.Constants.JcrName.Rbacl;
import static org.fcrepo.auth.roles.common.Constants.JcrName.assignment;
import static org.fcrepo.auth.roles.common.Constants.JcrName.principal;
import static org.fcrepo.auth.roles.common.Constants.JcrName.rbacl;
import static org.fcrepo.auth.roles.common.Constants.JcrName.rbaclAssignable;
import static org.fcrepo.auth.roles.common.Constants.JcrName.role;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides the effective access roles for authorization.
 *
 * @author Gregory Jansen
 */
@Component
public class AccessRolesProvider {

    private static final Logger LOGGER = getLogger(AccessRolesProvider.class);

    public static final Map<String, List<String>> DEFAULT_ACCESS_ROLES = emptyMap();

    /**
     * Get the roles assigned to this Node. Optionally search up the tree for
     * the effective roles.
     *
     * @param node the subject Node
     * @param effective if true then search for effective roles
     * @return a set of roles for each principal
     */
    public Map<String, List<String>> getRoles(Node node, final boolean effective) throws RepositoryException {
        final Map<String, List<String>> data = new HashMap<>();
        final Session session = node.getSession();
        registerPrefixes(session);
        if (node.isNodeType(rbaclAssignable.getQualified())) {
            getAssignments(node, data);
            return data;
        }
        if (effective) { // look up the tree
            try {
                for (node = node.getParent(); node != null; node =
                        node.getParent()) {
                    if (node.isNodeType(rbaclAssignable.getQualified())) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("effective roles are assigned at node: {}",
                                         node.getPath());
                        }
                        getAssignments(node, data);
                        if (LOGGER.isDebugEnabled()) {
                            for (final Map.Entry<String, List<String>> entry : data.entrySet()) {
                                LOGGER.debug("{} has role(s) {}", entry.getKey(), entry.getValue());
                            }
                        }
                        return data;
                    }
                }
            } catch (final ItemNotFoundException e) {
                LOGGER.trace("Subject not found, using default access roles", e);
                return DEFAULT_ACCESS_ROLES;
            }
        }
        return null;
    }

    /**
     * @param node
     * @param data
     * @throws RepositoryException
     */
    private void getAssignments(final Node node, final Map<String, List<String>> data)
        throws RepositoryException {

        if (node.isNodeType(rbaclAssignable.getQualified())) {
            try {
                final Node rbacl = node.getNode(JcrName.rbacl.getQualified());
                LOGGER.debug("got rbacl: {}", rbacl);
                for (final NodeIterator ni = rbacl.getNodes(); ni.hasNext();) {
                    final Node assign = ni.nextNode();
                    final String principalName =
                            assign.getProperty(principal.getQualified())
                                    .getString();
                    if (principalName == null ||
                            principalName.trim().length() == 0) {
                        LOGGER.warn("found empty principal name on node {}",
                                    node.getPath());
                    } else {
                        List<String> roles = data.get(principalName);
                        if (roles == null) {
                            roles = new ArrayList<>();
                            data.put(principalName, roles);
                        }
                        for (final Value v : assign.getProperty(
                                role.getQualified()).getValues()) {
                            if (v == null || v.toString().trim().length() == 0) {
                                LOGGER.warn("found empty role name on node {}",
                                            node.getPath());
                            } else {
                                roles.add(v.toString());
                            }
                        }
                    }
                }
            } catch (final PathNotFoundException e) {
                LOGGER.info(
                             "Found rbaclAssignable mixin without a corresponding node at {}",
                             node.getPath());
            }
        }
    }

    /**
     * Assigns the given set of roles to each principal.
     *
     * @param node the Node to edit
     * @param data the roles to assign
     */
    public void postRoles(final Node node, final Map<String, Set<String>> data)
        throws RepositoryException {
        final Session session = node.getSession();
        registerPrefixes(session);
        if (!node.isNodeType(rbaclAssignable.getQualified())) {
            node.addMixin(rbaclAssignable.getQualified());
            LOGGER.debug("added rbaclAssignable type");
        }

        Node acl;

        if (node.hasNode(rbacl.getQualified())) {
            acl = node.getNode(rbacl.getQualified());
            for (final NodeIterator ni = acl.getNodes(); ni.hasNext();) {
                ni.nextNode().remove();
            }
        } else {
            acl = node.addNode(rbacl.getQualified(), Rbacl.getQualified());
        }

        for (final Map.Entry<String, Set<String>> entry : data.entrySet()) {
            final Node assign = acl.addNode(assignment.getQualified(), Assignment.getQualified());
            assign.setProperty(principal.getQualified(), entry.getKey());
            assign.setProperty(role.getQualified(), toArray(entry.getValue(), String.class));
        }
    }

    /**
     * Deletes all roles assigned on this node and removes the mixin type.
     *
     * @param node
     */
    public void deleteRoles(final Node node) throws RepositoryException {
        final Session session = node.getSession();
        registerPrefixes(session);
        if (node.isNodeType(rbaclAssignable.getQualified())) {
            // remove rbacl child
            try {
                final Node rbacl = node.getNode(JcrName.rbacl.getQualified());
                rbacl.remove();
            } catch (final PathNotFoundException e) {
                LOGGER.debug("Cannot find node: {}", node, e);
            }
            // remove mixin
            node.removeMixin(rbaclAssignable.getQualified());
        }
    }

    /**
     * Finds effective roles assigned to a path, using first real ancestor node.
     *
     * @param absPath the real or potential node path
     * @return the roles assigned to each principal
     * @throws RepositoryException
     */
    public Map<String, List<String>> findRolesForPath(final Path absPath,
            final Session session) throws RepositoryException {
        Node node = null;
        for (Path p = absPath; p != null; p = p.getParent()) {
            try {
                if (p.isRoot()) {
                    node = session.getRootNode();
                } else {
                    node = session.getNode(p.getString());
                }
                break;
            } catch (final PathNotFoundException e) {
                LOGGER.debug("Cannot find node: {}", p, e);
            }
        }
        return this.getRoles(node, true);
    }

}
