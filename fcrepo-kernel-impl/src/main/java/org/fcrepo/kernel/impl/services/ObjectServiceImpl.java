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

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.exception.ResourceTypeException;
import org.fcrepo.kernel.impl.FedoraObjectImpl;
import org.fcrepo.kernel.services.ObjectService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Service for creating and retrieving FedoraObjects without using the JCR API.
 *
 * @author cbeer
 * @since Feb 11, 2013
 */
@Component
public class ObjectServiceImpl extends AbstractService implements ObjectService {

    private static final Logger LOGGER = getLogger(ObjectServiceImpl.class);

    /**
     * @param path
     * @param session
     * @return A FedoraObject with the proffered PID
     * @throws RepositoryException
     */
    @Override
    public FedoraObject findOrCreate(final Session session, final String path) {
        LOGGER.trace("Executing findOrCreateObject() with path: {}", path);

        try {
            final Node node = findOrCreateNode(session, path, NT_FOLDER);

            if (node.isNew()) {
                initializeNewObjectProperties(node);
            }

            return new FedoraObjectImpl(node);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Retrieve a FedoraObject instance by pid and dsid
     *
     * @param path
     * @return A FedoraObject with the proffered PID
     * @throws javax.jcr.RepositoryException
     */
    @Override
    public FedoraObject find(final Session session, final String path) {
        final Node node = findNode(session, path);

        return cast(node);
    }

    private void initializeNewObjectProperties(final Node node) {
        try {
            LOGGER.debug("Setting object properties on node {}...", node.getPath());

            if (node.canAddMixin(FEDORA_RESOURCE)) {
                node.addMixin(FEDORA_RESOURCE);
            }

            if (node.canAddMixin(FEDORA_OBJECT)) {
                node.addMixin(FEDORA_OBJECT);
            }

        } catch (final RepositoryException e) {
            LOGGER.warn("Could not decorate {} with {} properties: {} ",
                    JCR_CONTENT, FEDORA_OBJECT, e);
        }
    }

    @Override
    public FedoraObject cast(final Node node) {
        assertIsType(node);
        return new FedoraObjectImpl(node);
    }

    private void assertIsType(final Node node) {
        if (!FedoraObjectImpl.hasMixin(node)) {
            throw new ResourceTypeException(node + " can not be used as a object");
        }
    }

}
