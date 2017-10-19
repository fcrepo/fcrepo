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
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.NT_VERSION_FILE;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getContainingNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touch;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touchLdpMembershipResource;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;

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
        return findOrCreate(session, path, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FedoraBinary findOrCreate(final FedoraSession session, final String path, final Calendar mementoDatetime) {
        try {
            final Node dsNode = findOrCreateNode(session, path, NT_VERSION_FILE);

            if (dsNode.isNew()) {
                initializeNewDatastreamProperties(dsNode);

                if (mementoDatetime != null) {
                  initializeMementoProperties(dsNode, mementoDatetime);
                }

                getContainingNode(dsNode).ifPresent(parent -> {
                    touch(parent);
                    touchLdpMembershipResource(dsNode);
                });
            }

            final FedoraBinaryImpl binary = new FedoraBinaryImpl(dsNode.getNode(JCR_CONTENT));

            if (dsNode.isNew()) {
                touch(binary.getNode());
            }

            return binary;
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
        try {
            final Node dsNode = findNode(session, path);

            return cast(dsNode.getNode(JCR_CONTENT));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private static void initializeNewDatastreamProperties(final Node node) {
        try {

            if (node.canAddMixin(FEDORA_RESOURCE)) {
                node.addMixin(FEDORA_RESOURCE);
            }

            if (node.canAddMixin(FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
                node.addMixin(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
            }

            final Node contentNode = jcrTools.findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);

            if (contentNode.canAddMixin(FEDORA_BINARY)) {
                contentNode.addMixin(FEDORA_BINARY);
            }
        } catch (final RepositoryException e) {
            LOGGER.warn("Could not decorate {} with datastream properties: {}", node, e);
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
