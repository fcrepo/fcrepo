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

package org.fcrepo.kernel.services;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * @author bbpennel
 * @date Feb 19, 2014
 */
public interface VersionService extends Service {

    /**
     * Notifies the version manager that the node at a given path was updated so
     * that if automatic versioning is set for that node, a version checkpoint
     * will be made. When a node object is available, use of
     * {@link #nodeUpdated(javax.jcr.Node)} will save the overhead of a
     * redundant node lookup.
     *
     * @param session
     * @param absPath the absolute path to the node that was created or modified
     * @throws RepositoryException
     */
    void nodeUpdated(Session session, String absPath)
        throws RepositoryException;

    /**
     * Notifies the version manager that the given node was updated so that if
     * automatic versioning is set for that node, a version checkpoint will be
     * made.
     *
     * @param n the node that was updated
     * @throws RepositoryException
     */
    void nodeUpdated(Node n) throws RepositoryException;

    /**
     * Explicitly creates a version for the nodes at each path provided.
     *
     * @param workspace the workspace in which the node resides
     * @param paths absolute paths to the nodes within the workspace
     * @throws RepositoryException
     */
    void createVersion(Workspace workspace, Collection<String> paths)
        throws RepositoryException;

    /**
     * Reverts the node to the version identified by the label.  This method
     * will throw a PathNotFoundException if no version with the given label is
     * found.
     *
     * @param workspace the workspace in which the node resides
     * @param absPath the path to the node whose version is to be reverted
     * @param label identifies the historic version
     * @throws RepositoryException
     */
    void revertToVersion(Workspace workspace, String absPath, String label)
        throws RepositoryException;

    /**
     * Creates a version checkpoint for the given node if versioning is enabled
     * for that node type. When versioning is enabled this is the equivalent of
     * VersionManager#checkpoint(node.getPath()), except that it is aware of
     * TxSessions and queues these operations accordingly.
     *
     * @param node the node for whom a new version is to be minted
     * @throws RepositoryException
     */
    void checkpoint(Node node) throws RepositoryException;

    /**
     * @param txService the txService to set
     */
    void setTxService(final TransactionService txService);
}