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
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.FedoraExternalContent.PROXY;
import static org.fcrepo.kernel.api.FedoraExternalContent.REDIRECT;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_DESCRIPTION;
import static org.fcrepo.kernel.api.RdfLexicon.NT_LEAF_NODE;
import static org.fcrepo.kernel.api.RdfLexicon.NT_VERSION_FILE;
import static org.fcrepo.kernel.api.RdfLexicon.PROXY_FOR;
import static org.fcrepo.kernel.api.RdfLexicon.REDIRECTS_TO;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getContainingNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touch;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touchLdpMembershipResource;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/10/14
 */
@Component
public class BinaryServiceImpl extends AbstractService implements BinaryService {

    private static final Logger LOGGER = getLogger(BinaryServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public FedoraBinary findOrCreate(final FedoraSession session, final String path) {
        try {
            final FedoraBinary binary = findOrCreateBinary(session, path);

            final Node dsNode = getJcrNode(binary);
            if (dsNode.isNew()) {
                final String descPath = dsNode.getPath() + "/" + FEDORA_DESCRIPTION;
                findOrCreateDescription(session, descPath);
            }

            return binary;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FedoraBinary findOrCreateBinary(final FedoraSession session, final String path) {
        try {
            final Node dsNode = findOrCreateNode(session, path, NT_VERSION_FILE);

            if (dsNode.isNew()) {
                initializeNewBinaryProperties(dsNode);

                getContainingNode(dsNode).ifPresent(parent -> {
                    touch(parent);
                    touchLdpMembershipResource(dsNode);
                });
            }

            final FedoraBinaryImpl binary = new FedoraBinaryImpl(dsNode);

            if (dsNode.isNew()) {
                touch(binary.getNode());
            }

            return binary;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public FedoraBinary findOrCreate(final FedoraSession session, final String path, final String externalHandling,
                                     final String externalUrl) {
        try {
            final Node dsNode = findOrCreateNode(session, path, NT_VERSION_FILE);

            if (dsNode.isNew()) {
                if (externalHandling != null && externalUrl != null) {
                    initializeNewExternalNodeProperties(dsNode, externalHandling, externalUrl);
                }
                initializeNewDatastreamProperties(dsNode);

                getContainingNode(dsNode).ifPresent(parent -> {
                    touch(parent);
                    touchLdpMembershipResource(dsNode);
                });
            }

            final FedoraBinaryImpl binary = new FedoraBinaryImpl(dsNode);

            if (dsNode.isNew()) {
                touch(binary.getNode());
            }

            return binary;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NonRdfSourceDescription findOrCreateDescription(final FedoraSession session, final String path) {
        try {
            final Node descNode = findOrCreateNode(session, path, NT_LEAF_NODE);

            initializeNewDescriptionProperties(descNode);

            return new NonRdfSourceDescriptionImpl(descNode);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param path jcr path to the datastream
     * @return datastream
     */
    @Override
    public FedoraBinary find(final FedoraSession session, final String path) {
        return cast(findNode(session, path));
    }

    private static void initializeNewExternalNodeProperties(final Node node, final String extHandling,
                                                            final String extUrl) {
        try {
            if (extHandling.equals(PROXY)) {
                node.setProperty(PROXY_FOR.toString(), extUrl);
            } else if (extHandling.equals(REDIRECT)) {
                node.setProperty(REDIRECTS_TO.toString(), extUrl);
            } else {
                throw new RepositoryException("Unknown External Handling type: " + extHandling);
            }
        } catch (RepositoryException e) {
            LOGGER.warn("Could not initialize {} with external handling properties: {}", node, e);
        }
    }

    private static void initializeNewDatastreamProperties(final Node node) {
        try {

            if (node.canAddMixin(FEDORA_RESOURCE)) {
                node.addMixin(FEDORA_RESOURCE);
            }
            if (node.canAddMixin(FEDORA_BINARY)) {
                node.addMixin(FEDORA_BINARY);
            }
        } catch (final RepositoryException e) {
            LOGGER.warn("Could not decorate {} with binary properties: {}", node, e);
        }
    }

    private static void initializeNewDescriptionProperties(final Node descNode) {
        try {
            if (descNode.canAddMixin(FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
                descNode.addMixin(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
            }

            if (descNode.canAddMixin(FEDORA_BINARY)) {
                descNode.addMixin(FEDORA_BINARY);
            }
        } catch (final RepositoryException e) {
            LOGGER.warn("Could not decorate {} with description properties: {}", descNode, e);
        }
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return node as datastream
     */
    private FedoraBinary cast(final Node node) {
        assertIsType(node);
        return new FedoraBinaryImpl(node);
    }

    private static void assertIsType(final Node node) {
        if (!FedoraBinaryImpl.hasMixin(node)) {
            throw new ResourceTypeException(node + " can not be used as a binary");
        }
    }


}
