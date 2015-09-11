/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.services;

import org.fcrepo.kernel.api.exception.ForbiddenResourceModificationException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.modeshape.TombstoneImpl;
import org.modeshape.jcr.api.JcrTools;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;


/**
 * @author bbpennel
 * @author ajs6f
 * @since Feb 20, 2014
 */
public class AbstractService {
    protected final static JcrTools jcrTools = new JcrTools();

    protected Node findOrCreateNode(final Session session,
                                    final String path,
                                    final String finalNodeType) throws RepositoryException {

        final Node preexistingNode = getClosestExistingAncestor(session, path);

        if (TombstoneImpl.hasMixin(preexistingNode)) {
            throw new TombstoneException(new TombstoneImpl(preexistingNode));
        }

        // Nodes should not be created as children of existing PairTree resources
        if (isImmediateParent(preexistingNode.getPath(), path) && preexistingNode.isNodeType(FEDORA_PAIRTREE)) {
            throw new ForbiddenResourceModificationException("Resources cannot be created under pairtree elements!");
        }

        final Node node = jcrTools.findOrCreateNode(session, path, NT_FOLDER, finalNodeType);

        if (node.isNew()) {
            tagHierarchyWithPairtreeMixin(preexistingNode, node);
        }

        return node;
    }

    /**
     * Returns true if arg 'possibleParent' path matches arg 'path' minus the last path segment.
     *
     * @param possibleParent of the arg 'path'
     * @param path           whose parent path will be compared to the arg 'possibleParent'
     * @return true if 'possibleParent' equals 'path' minus the last path segment
     */
    private boolean isImmediateParent(final String possibleParent, final String path) {
        final String possibleChild = path.startsWith("/") ? path : "/" + path;
        return possibleParent.equals(possibleChild.substring(0, possibleChild.lastIndexOf('/')));
    }

    protected Node findNode(final Session session, final String path) {
        try {
            return session.getNode(path);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Tag a hierarchy with {@link org.fcrepo.kernel.api.FedoraJcrTypes#FEDORA_PAIRTREE}
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
    public boolean exists(final Session session, final String path) {
        try {
            return session.nodeExists(path);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
