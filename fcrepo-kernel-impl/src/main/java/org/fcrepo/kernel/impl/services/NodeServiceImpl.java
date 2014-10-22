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
package org.fcrepo.kernel.impl.services;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.impl.rdf.impl.NodeTypeRdfContext;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Service for managing access to Fedora 'nodes' (either datastreams or objects,
 * we don't care.)
 *
 * @author Chris Beer
 * @since May 9, 2013
 */
@Component
public class NodeServiceImpl extends AbstractService implements NodeService {

    private static final Logger LOGGER = getLogger(NodeServiceImpl.class);

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.services.Service#exists(javax.jcr.Session, java.lang.String)
     */
    @Override
    public boolean exists(final Session session, final String path) {
        try {
            validatePath(session, path);
            return session.nodeExists(path);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Retrieve an existing Fedora resource at the given path
     *
     * @param session a JCR session
     * @param path a JCR path
     * @return Fedora resource at the given path
     * @throws RepositoryException
     */
    @Override
    public FedoraResource getObject(final Session session, final String path) {
        try {
            return new FedoraResourceImpl(session.getNode(path));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Copy an existing object from the source path to the destination path
     *
     * @param session
     * @param source
     * @param destination
     * @throws RepositoryException
     */
    @Override
    public void copyObject(final Session session, final String source, final String destination) {
        try {
            session.getWorkspace().copy(source, destination);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Move an existing object from the source path to the destination path
     *
     * @param session
     * @param source
     * @param destination
     * @throws RepositoryException
     */
    @Override
    public void moveObject(final Session session, final String source, final String destination) {
        try {
            session.getWorkspace().move(source, destination);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * @param session
     * @return node types
     * @throws RepositoryException
     */
    @Override
    public RdfStream getNodeTypes(final Session session) {
        try {
            return new NodeTypeRdfContext(session.getWorkspace().getNodeTypeManager());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * @param session
     * @param cndStream
     * @throws RepositoryException
     * @throws IOException
     */
    @Override
    public void registerNodeTypes(final Session session, final InputStream cndStream) throws IOException {
        try {
            final NodeTypeManager nodeTypeManager = (NodeTypeManager) session.getWorkspace().getNodeTypeManager();
            nodeTypeManager.registerNodeTypes(cndStream, true);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Validate resource path for unregistered namespace prefixes
     *
     * @param session the JCR session to use
     * @param path the absolute path to the object
     * @throws org.fcrepo.kernel.exception.FedoraInvalidNamespaceException on unregistered namespaces
     * @throws RepositoryRuntimeException
     */
    private void validatePath(final Session session, final String path) {

        final NamespaceRegistry namespaceRegistry;
        try {
            namespaceRegistry =
                    session.getWorkspace().getNamespaceRegistry();
            checkNotNull(namespaceRegistry,
                    "Couldn't find namespace registry in repository!");
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        final String relPath = path.replaceAll("^/+", "").replaceAll("/+$", "");
        final String[] pathSegments = relPath.split("/");
        for (final String segment : pathSegments) {
            if (segment.length() > 0 && segment.contains(":") &&
                    segment.substring(0, segment.indexOf(":")) != "fedora") {
                final String prefix = segment.substring(0, segment.indexOf(":"));
                try {
                    namespaceRegistry.getURI(prefix);
                } catch (final NamespaceException e) {
                    throw new FedoraInvalidNamespaceException(
                            String.format("The namespace prefix (%s) has not been registered", prefix), e);
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        }
    }

}
