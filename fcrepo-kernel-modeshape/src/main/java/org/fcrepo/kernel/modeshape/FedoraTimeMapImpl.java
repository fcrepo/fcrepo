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
package org.fcrepo.kernel.modeshape;

import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraTimeMap;

/**
 * @author lsitu
 * @since Oct. 04, 2017
 */
public class FedoraTimeMapImpl extends FedoraResourceImpl implements FedoraTimeMap {

    /**
     * Construct a {@link org.fcrepo.kernel.api.models.FedoraResource} from an existing JCR Node
     * @param node an existing JCR node to be treated as a fcrepo object
     */
    public FedoraTimeMapImpl(final Node node) {
        super(node);
    }

    @Override
    public void delete() {
        try {
            // check to remove references to the TimeMap if it exists
            removeReferences(node);

            // delete the TimeMap node
            node.remove();
        } catch (final javax.jcr.AccessDeniedException e) {
            throw new AccessDeniedException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Check if the JCR node has a fedora:TimeMap mixin
     * @param node the JCR node
     * @return true if the JCR node has the fedora:TimeMap mixin
     */
    public static boolean hasMixin(final Node node) {
        try {
            return node.isNodeType(FEDORA_TIME_MAP);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public FedoraResource getOriginalResource() {
        try {
            final Property originalProperty = node.getProperty(MEMENTO_ORIGINAL);
            final Node originalNode = originalProperty.getNode();

            return Optional.of(originalNode).map(nodeConverter::convert).orElse(null);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
