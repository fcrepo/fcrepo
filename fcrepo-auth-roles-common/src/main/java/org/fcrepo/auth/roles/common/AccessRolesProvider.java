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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.auth.roles.common.Constants.JcrName;
import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.google.common.collect.Iterables.toArray;
import static org.fcrepo.auth.roles.common.Constants.registerPrefixes;
import static org.fcrepo.auth.roles.common.Constants.JcrName.principal;
import static org.fcrepo.auth.roles.common.Constants.JcrName.rbaclAssignable;

/**
 * Provides the effective access roles for authorization.
 *
 * @author Gregory Jansen
 */
@Component
public class AccessRolesProvider {

    private static final Logger LOGGER = LoggerFactory
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
        Map<String, List<String>> data =
                new HashMap<>();
        final Session session = node.getSession();
        registerPrefixes(session);
        if (node.isNodeType(JcrName.rbaclAssignable.getQualified())) {
            getAssignments(node, data);
        }
        if (effective) { // look up the tree
            try {
                for (node = node.getParent(); node != null; node =
                        node.getParent()) {
                    if (node.isNodeType(JcrName.rbaclAssignable
                            .getQualified())) {
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
                    }
                }
            } catch (final ItemNotFoundException e) {
                LOGGER.trace("Subject not found, using default access roles", e);
                return DEFAULT_ACCESS_ROLES;
            }
        } else {
        }
        if (data.size()==0) data = null;
        return data;
    }

    /**
     * @param node
     * @param data
     * @throws RepositoryException
     */
    private void getAssignments(final Node node, final Map<String, List<String>> data)
        throws RepositoryException {

        if (node.isNodeType(rbaclAssignable.getQualified())) {
            int cnt = 1;
            while (true) {
                try {
                    Property property = node.getProperty(JcrName.principal.getQualified() + Integer.toString(cnt));
                    if (property == null) break;
                } catch (PathNotFoundException ex) {
                    break;
                }
                try {
                    final String principalName =
                        node.getProperty(principal.getQualified() + Integer.toString(cnt))
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
                        for (final Value v : node.getProperty(
                            JcrName.role.getQualified() + Integer.toString(cnt)).getValues()) {
                            if (v == null || v.toString().trim().length() == 0) {
                                LOGGER.warn("found empty role name on node {}",
                                    node.getPath());
                            } else {
                                roles.add(v.toString());
                            }
                        }
                    }
                } catch (final PathNotFoundException e) {
                    LOGGER.error(
                             "Found rbaclAssignable mixin without a corresponding node at {}",
                             node.getPath(),
                             e);
                }
                cnt++;
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
        deleteRoles(node);
        if (!node.isNodeType(JcrName.rbaclAssignable.getQualified())) {
            node.addMixin(JcrName.rbaclAssignable.getQualified());
            LOGGER.debug("added rbaclAssignable type");
        }

        int cnt = 1;
        for (final Map.Entry<String, Set<String>> entry : data.entrySet()) {
            node.setProperty(JcrName.principal.getQualified() + Integer.toString(cnt), entry.getKey());
            node.setProperty(JcrName.role.getQualified() + Integer.toString(cnt),
                    toArray(entry.getValue(), String.class));
            cnt++;
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
            // remove mixin
            int cnt = 1;
            while (true) {
                try {
                    Property property = node.getProperty(JcrName.principal.getQualified() + Integer.toString(cnt));
                    if (property == null) break;
                } catch (PathNotFoundException ex) {
                    break;
                }
                node.setProperty(JcrName.principal.getQualified() + Integer.toString(cnt), (String) null);
                node.setProperty(JcrName.role.getQualified() + Integer.toString(cnt),(String) null);
                cnt++;
            }
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
                LOGGER.debug("Cannot find node: {}", p, e);
            }
        }
        return this.getRoles(node, true);
    }

}
