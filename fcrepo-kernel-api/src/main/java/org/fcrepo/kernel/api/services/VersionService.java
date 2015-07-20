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
package org.fcrepo.kernel.api.services;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author bbpennel
 * @since Feb 19, 2014
 */
public interface VersionService {

    /**
     * Explicitly creates a version for the nodes at each path provided.
     *
     * @param session the session in which the node resides
     * @param absPath absolute paths to the node
     * @param label a label to be applied to the new version
     * @throws RepositoryException if repository exception occurred
     * @return the identifier
     */
    String createVersion(Session session, String absPath, String label) throws RepositoryException;

    /**
     * Reverts the node to the version identified by the label.  This method
     * will throw a PathNotFoundException if no version with the given label is
     * found.
     *
     * @param session the session in which the node resides
     * @param absPath the path to the node whose version is to be reverted
     * @param label identifies the historic version
     * @throws RepositoryException if repository exception occurred
     */
    void revertToVersion(Session session, String absPath, String label)
        throws RepositoryException;

    /**
     * Remove a version of a node.  This method will throw a PathNotFoundException
     * if no version with the given label is found.
     *
     * @param session the session in which the node resides
     * @param absPath the path to the node whose version is to be removed
     * @param label identifies the historic version by label or id
     * @throws RepositoryException if repository exception occurred
     */
    void removeVersion(Session session, String absPath, String label)
        throws RepositoryException;

}
