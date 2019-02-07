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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TIME_MAP;
import static org.fcrepo.kernel.api.FedoraTypes.MEMENTO_ORIGINAL;
import static org.fcrepo.kernel.api.RdfLexicon.LDPCV_TIME_MAP;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import org.fcrepo.kernel.api.models.FedoraTimeMap;
import org.fcrepo.kernel.api.services.TimeMapService;
import org.fcrepo.kernel.modeshape.FedoraTimeMapImpl;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;


/**
 * Service for creating and retrieving {@link org.fcrepo.kernel.api.models.FedoraTimeMap} without using the JCR API.
 *
 * @author bbpennel
 */
@Component
public class TimeMapServiceImpl extends AbstractService implements TimeMapService {

    private static final Logger LOGGER = getLogger(TimeMapServiceImpl.class);

    @Override
    public FedoraTimeMap find(final FedoraSession session, final String path) {
        final String ldpcvPath = getLdpcvPath(path);

        return cast(findNode(session, ldpcvPath));
    }

    @Override
    public FedoraTimeMap findOrCreate(final FedoraSession session, final String path) {
        try {
            // Add fedora:timemap to path if not present
            final String ldpcvPath = getLdpcvPath(path);

            final Node node = findOrCreateNode(session, ldpcvPath, NT_FOLDER);

            if (node.isNew()) {
                LOGGER.debug("Created TimeMap LDPCv {}", node.getPath());

                // add mixin type fedora:Resource
                if (node.canAddMixin(FEDORA_RESOURCE)) {
                    node.addMixin(FEDORA_RESOURCE);
                }

                // add mixin type fedora:TimeMap
                if (node.canAddMixin(FEDORA_TIME_MAP)) {
                    node.addMixin(FEDORA_TIME_MAP);
                }

                // Set reference from timegate/map to original resource
                node.setProperty(MEMENTO_ORIGINAL, node.getParent());
            }

            return new FedoraTimeMapImpl(node);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean exists(final FedoraSession session, final String path) {
        final String ldpcvPath = getLdpcvPath(path);
        return super.exists(session, ldpcvPath);
    }

    private static final String getLdpcvPath(final String path) {
        if (path.endsWith("/" + LDPCV_TIME_MAP)) {
            return path;
        } else {
            return path.replaceFirst("/*$", "") + "/" + LDPCV_TIME_MAP;
        }
    }

    private FedoraTimeMap cast(final Node node) {
        assertIsType(node);
        return new FedoraTimeMapImpl(node);
    }

    private static void assertIsType(final Node node) {
        if (!FedoraTimeMapImpl.hasMixin(node)) {
            throw new ResourceTypeException(node + " can not be used as a timemap");
        }
    }
}
