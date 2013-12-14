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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemExistsException;
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
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.google.common.collect.Iterables.toArray;

/**
 * Provides the effective access roles for authorization.
 *
 * @author Gregory Jansen
 */
@Component
public class AccessRolesProvider {

    private static final Logger log = LoggerFactory
            .getLogger(AccessRolesProvider.class);

    public static final Map<String, List<String>> DEFAULT_ACCESS_ROLES =
            Collections.emptyMap();

    /**
     * Get the roles assigned to this Node. Optionally search up the tree for
     * the effective roles.
     *
     * @param node the subject Node
     * @param effective if true then search for effective roles
     * @return a set of roles for each principal
     */
    public Map<String, List<String>>
    getRoles(Node node, final boolean effective)
        throws RepositoryException {
        final Map<String, List<String>> data =
                new HashMap<String, List<String>>();
        final Session session = node.getSession();
        Constants.registerPrefixes(session);
        if (node.isNodeType(JcrName.rbaclAssignable.getQualified())) {
            getAssignments(node, data);
            return data;
        } else {
            if (effective) { // look up the tree
                try {
                    for (node = node.getParent(); node != null; node =
                            node.getParent()) {
                        if (node.isNodeType(JcrName.rbaclAssignable
                                .getQualified())) {
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "effective roles are assigned at node: {}",
                                        node.getPath());
                            }
                            getAssignments(node, data);
                            if (log.isDebugEnabled()) {
                                for (final String key : data.keySet()) {
                                    log.debug("{} has role(s) {}", key, data
                                            .get(key));
                                }
                            }
                            return data;
                        }
                    }
                } catch (final ItemNotFoundException e) {
                    return DEFAULT_ACCESS_ROLES;
                }
            }
            return null;
        }
    }

    /**
     * @param node
     * @param data
     * @throws RepositoryException
     */
    private void getAssignments(final Node node,
            final Map<String, List<String>> data)
        throws RepositoryException {

        if (node.isNodeType(JcrName.rbaclAssignable.getQualified())) {
            try {
                final Node rbacl = node.getNode(JcrName.rbacl.getQualified());
                log.debug("got rbacl: {}", rbacl);
                for (final NodeIterator ni = rbacl.getNodes(); ni.hasNext();) {
                    final Node assign = ni.nextNode();
                    final String principalName =
                            assign.getProperty(JcrName.principal.getQualified())
                                    .getString();
                    if (principalName == null ||
                            principalName.trim().length() == 0) {
                        log.warn("found empty principal name on node {}",
                                node.getPath());
                    } else {
                        List<String> roles = data.get(principalName);
                        if (roles == null) {
                            roles = new ArrayList<String>();
                            data.put(principalName, roles);
                        }
                        for (final Value v : assign.getProperty(
                                JcrName.role.getQualified()).getValues()) {
                            if (v == null || v.toString().trim().length() == 0) {
                                log.warn("found empty role name on node {}",
                                        node.getPath());
                            } else {
                                roles.add(v.toString());
                            }
                        }
                    }
                }
            } catch (final PathNotFoundException e) {
                log.error(
                        "Found rbaclAssignable mixin without a corresponding node at {}",
                                node.getPath(),
                        e);
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
        Constants.registerPrefixes(session);
        if (!node.isNodeType(JcrName.rbaclAssignable.getQualified())) {
            node.addMixin(JcrName.rbaclAssignable.getQualified());
            log.debug("added rbaclAssignable type");
        }

        Node acl = null;
        try {
            acl =
                    node.addNode(JcrName.rbacl.getQualified(), JcrName.Rbacl
                            .getQualified());
        } catch (final ItemExistsException e) {
            acl = node.getNode(JcrName.rbacl.getQualified());
            for (final NodeIterator ni = acl.getNodes(); ni.hasNext();) {
                ni.nextNode().remove();
            }
        }

        for (final Map.Entry<String, Set<String>> entry : data.entrySet()) {
            final Node assign =
                    acl.addNode(JcrName.assignment.getQualified(),
                            JcrName.Assignment.getQualified());
            assign.setProperty(JcrName.principal.getQualified(), entry.getKey());
            assign.setProperty(JcrName.role.getQualified(), toArray(entry.getValue(), String.class));
        }
    }

    /**
     * Deletes all roles assigned on this node and removes the mixin type.
     *
     * @param node
     */
    public void deleteRoles(final Node node) throws RepositoryException {
        final Session session = node.getSession();
        Constants.registerPrefixes(session);
        if (node.isNodeType(JcrName.rbaclAssignable.getQualified())) {
            // remove rbacl child
            try {
                final Node rbacl = node.getNode(JcrName.rbacl.getQualified());
                rbacl.remove();
            } catch (final PathNotFoundException e) {
            }
            // remove mixin
            node.removeMixin(JcrName.rbaclAssignable.getQualified());
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
                log.debug("Cannot find node: {}", p);
            }
        }
        return this.getRoles(node, true);
    }

}
