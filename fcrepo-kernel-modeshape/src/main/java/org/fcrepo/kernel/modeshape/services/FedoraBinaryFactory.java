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

import static org.fcrepo.kernel.api.utils.MessageExternalBodyContentType.isExternalBodyType;
import static org.fcrepo.kernel.api.FedoraTypes.HAS_MIME_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.utils.MessageExternalBodyContentType;
import org.fcrepo.kernel.modeshape.FedoraBinaryImpl;
import org.fcrepo.kernel.modeshape.LocalFileBinaryImpl;
import org.fcrepo.kernel.modeshape.UrlBinaryImpl;
import org.slf4j.Logger;

/**
 * Factory producing instances of FedoraBinary objects
 *
 * @author bbpennel
 */
public class FedoraBinaryFactory {
    private static final Logger LOGGER = getLogger(FedoraBinaryFactory.class);

    private static final String LOCAL_FILE_ACCESS_TYPE = "local-file";

    private static final String URL_ACCESS_TYPE = "url";

    private FedoraBinaryFactory() {
    }

    /**
     * Gets a FedoraBinary object for the given node
     *
     * @param node the node
     * @return new FedoraBinary object for the node
     */
    public static FedoraBinary getBinary(final Node node) {
        try {
            final String mimeType;
            if (node.hasProperty(HAS_MIME_TYPE)) {
                mimeType = node.getProperty(HAS_MIME_TYPE).getString();
            } else {
                mimeType = null;
            }

            return getBinary(node, mimeType);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Get a FedoraBinary object for the given node
     *
     * @param node node
     * @param mimeType mimetype of datastream
     * @return new FedoraBinary object for node
     */
    public static FedoraBinary getBinary(final Node node, final String mimeType) {
        final String accessType = mimeType == null ? null : getAccessType(mimeType);

        if (accessType != null) {
            if (LOCAL_FILE_ACCESS_TYPE.equals(accessType)) {
                LOGGER.debug("Instantiating local file FedoraBinary");
                return new LocalFileBinaryImpl(node);
            } else if (URL_ACCESS_TYPE.equals(accessType)) {
                LOGGER.debug("Instantiating URI FedoraBinary");
                return new UrlBinaryImpl(node);
            }
        }

        return new FedoraBinaryImpl(node);
    }

    private static String getAccessType(final String mimeType) {
        try {
            if (isExternalBodyType(mimeType)) {
                return MessageExternalBodyContentType.parse(mimeType).getAccessType();
            }
        } catch (final UnsupportedAccessTypeException e) {
            LOGGER.debug("Node did not have a valid mimetype for an external binary");
        }
        return null;
    }
}
