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
package org.fcrepo.kernel.modeshape.services;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.modeshape.TombstoneImpl;
import org.modeshape.jcr.api.JcrTools;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;


/**
 * @author bbpennel
 * @author ajs6f
 * @since Feb 20, 2014
 */
public class AbstractService {
    protected final static JcrTools jcrTools = new JcrTools();

    protected Node findOrCreateNode(final FedoraSession session,
                                    final String path,
                                    final String finalNodeType) throws RepositoryException {

        final Session jcrSession = getJcrSession(session);
        final Node preexistingNode = getClosestExistingAncestor(jcrSession, path);

        if (TombstoneImpl.hasMixin(preexistingNode)) {
            throw new TombstoneException(new TombstoneImpl(preexistingNode));
        }

        final Node node = jcrTools.findOrCreateNode(jcrSession, path, NT_FOLDER, finalNodeType);

        if (node.isNew()) {
            tagHierarchyWithPairtreeMixin(preexistingNode, node);
        }

        return node;
    }

    protected Node findNode(final FedoraSession session, final String path) {
        final Session jcrSession = getJcrSession(session);
        try {
            return jcrSession.getNode(path);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Tag a hierarchy with {@link org.fcrepo.kernel.api.FedoraTypes#FEDORA_PAIRTREE}
     * @param baseNode Top most ancestor that should not be tagged
     * @param createdNode Node whose parents should be tagged up to but not including {@code baseNode}
     * @throws RepositoryException if repository exception occurred
     */
    public static void tagHierarchyWithPairtreeMixin(final Node baseNode, final Node createdNode)
            throws RepositoryException {
        Node parent = createdNode.getParent();

        while (parent.isNew() && !parent.equals(baseNode)) {
            parent.addMixin(FEDORA_PAIRTREE);
            parent = parent.getParent();
        }
    }

    /** test node existence at path
     *
     * @param session the session
     * @param path the path
     * @return whether T exists at the given path
     */
    public boolean exists(final FedoraSession session, final String path) {
        final Session jcrSession = getJcrSession(session);
        try {
            return jcrSession.nodeExists(path);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
