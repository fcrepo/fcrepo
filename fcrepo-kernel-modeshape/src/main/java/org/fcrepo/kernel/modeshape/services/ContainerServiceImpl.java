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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.modeshape.ContainerImpl.hasMixin;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getContainingNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touch;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touchLdpMembershipResource;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.ContainerImpl;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Service for creating and retrieving {@link org.fcrepo.kernel.api.models.Container} without using the JCR API.
 *
 * @author cbeer
 * @author ajs6f
 * @since Feb 11, 2013
 */
@Component
public class ContainerServiceImpl extends AbstractService implements ContainerService {

    private static final Logger LOGGER = getLogger(ContainerServiceImpl.class);

    /**
     * @param path the path
     * @param session the session
     * @return A {@link org.fcrepo.kernel.api.models.Container} with the proffered PID
     */
    @Override
    public Container findOrCreate(final FedoraSession session, final String path) {
        return findOrCreate(session, path, null);
    }

    /**
     * @param path the path
     * @param session the session
     * @return A {@link org.fcrepo.kernel.api.models.Container} with the proffered PID
     */
    @Override
    public Container findOrCreate(final FedoraSession session, final String path, final Calendar mementoDatetime) {
        LOGGER.trace("Executing findOrCreateObject() with path: {}", path);

        try {
            final Node node = findOrCreateNode(session, path, NT_FOLDER);

            if (node.isNew()) {
                initializeNewObjectProperties(node);

                if (mementoDatetime != null) {
                  initializeMementoProperties(node, mementoDatetime);
                }

                getContainingNode(node).ifPresent(parent -> {
                    touch(parent);
                    touchLdpMembershipResource(node);
                });
            }

            if (node.isNew()) {
                touch(node);
            }

            return new ContainerImpl(node);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Retrieve a {@link org.fcrepo.kernel.api.models.Container} instance by pid and dsid
     *
     * @param path the path
     * @param session the session
     * @return A {@link org.fcrepo.kernel.api.models.Container} with the proffered PID
     */
    @Override
    public Container find(final FedoraSession session, final String path) {
        final Node node = findNode(session, path);

        return cast(node);
    }

    private static void initializeNewObjectProperties(final Node node) {
        try {
            LOGGER.debug("Setting object properties on node {}...", node.getPath());

            if (node.canAddMixin(FEDORA_RESOURCE)) {
                node.addMixin(FEDORA_RESOURCE);
            }

            if (node.canAddMixin(FEDORA_CONTAINER)) {
                node.addMixin(FEDORA_CONTAINER);
            }

        } catch (final RepositoryException e) {
            LOGGER.warn("Could not decorate {} with {} properties: {} ",
                    JCR_CONTENT, FEDORA_CONTAINER, e);
        }
    }

    private Container cast(final Node node) {
        assertIsType(node);
        return new ContainerImpl(node);
    }

    private static void assertIsType(final Node node) {
        if (!hasMixin(node)) {
            throw new ResourceTypeException(node + " can not be used as a object");
        }
    }

}
