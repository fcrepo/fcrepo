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
package org.fcrepo.kernel.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeIterator;

import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.utils.iterators.RdfStream;

/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface NodeService extends Service {

    /**
     * Find or create a new Fedora resource at the given path
     *
     * @param session
     * @param path
     * @return
     * @throws RepositoryException
     */
    FedoraResource findOrCreateObject(Session session, String path) throws RepositoryException;

    /**
     * Retrieve an existing Fedora resource at the given path
     *
     * @param session
     * @param path
     * @return
     * @throws RepositoryException
     */
    FedoraResource getObject(Session session, String path) throws RepositoryException;

    /**
     * Get an existing Fedora resource at the given path with the given version
     * label
     *
     * @param session
     * @param path
     * @param versionId a version label
     * @return
     * @throws RepositoryException
     */
    FedoraResource getObject(Session session, String path, String versionId) throws RepositoryException;

    /**
     * @return A Set of object names (identifiers)
     * @throws RepositoryException
     */
    Set<String> getObjectNames(Session session, String path) throws RepositoryException;

    /**
     * Get the list of children at the given path filtered by the given mixin
     *
     * @param session
     * @param path
     * @param mixin
     * @return
     * @throws RepositoryException
     */
    Set<String> getObjectNames(Session session, String path, String mixin) throws RepositoryException;

    /**
     * Delete an existing object from the repository at the given path
     *
     * @param session
     * @param path
     * @throws RepositoryException
     */
    void deleteObject(Session session, String path) throws RepositoryException;

    /**
     * Copy an existing object from the source path to the destination path
     * @param session
     * @param source
     * @param destination
     * @throws RepositoryException
     */
    void copyObject(Session session, String source, String destination) throws RepositoryException;

    /**
     * Move an existing object from the source path to the destination path
     * @param session
     * @param source
     * @param destination
     * @throws RepositoryException
     */
    void moveObject(Session session, String source, String destination) throws RepositoryException;

    /**
     * Get the full list of node types in the repository
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    NodeTypeIterator getAllNodeTypes(final Session session) throws RepositoryException;

    /**
     * @param session
     * @return
     * @throws RepositoryException
     */
    RdfStream getNodeTypes(final Session session) throws RepositoryException;

    /**
     * @param session
     * @param cndStream
     * @throws RepositoryException
     * @throws IOException
     */
    void registerNodeTypes(final Session session, final InputStream cndStream) throws RepositoryException,
        IOException;

}