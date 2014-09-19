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

import javax.inject.Inject;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.exception.FedoraInvalidNamespaceException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.services.Service;

import org.modeshape.jcr.api.JcrTools;


/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public abstract class AbstractService extends JcrTools implements FedoraJcrTypes, Service {

    @Inject
    protected Repository repo;

    /**
     * Set the repository to back this RepositoryService
     *
     * @param repository
     */
    @Override
    public void setRepository(final Repository repository) {
        repo = repository;
    }

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
     * Validate resource path for unregistered namespace prefixes
     *
     * @param session the JCR session to use
     * @param path the absolute path to the object
     * @throws FedoraInvalidNamespaceException on unregistered namespaces
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
