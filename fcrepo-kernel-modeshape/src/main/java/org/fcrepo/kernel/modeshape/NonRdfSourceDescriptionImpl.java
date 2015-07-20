/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape;

import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isNonRdfSourceDescription;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.NonRdfSource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.slf4j.Logger;

/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 *
 * @author ajs6f
 * @since Feb 21, 2013
 */
public class NonRdfSourceDescriptionImpl extends FedoraResourceImpl implements NonRdfSourceDescription {

    private static final Logger LOGGER = getLogger(NonRdfSourceDescriptionImpl.class);

    /**
     * The JCR node for this datastream
     *
     * @param n an existing {@link Node}
     */
    public NonRdfSourceDescriptionImpl(final Node n) {
        super(n);
    }

    @Override
    public NonRdfSource getDescribedResource() {
        return new FedoraBinaryImpl(getContentNode());
    }

    private Node getContentNode() {
        LOGGER.trace("Retrieved datastream content node.");
        try {
            return node.getNode(JCR_CONTENT);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Check if the node has a fedora:datastream mixin
     *
     * @param node node to check
     * @return whether the node has a fedora:datastream mixin
     */
    public static boolean hasMixin(final Node node) {
        return isNonRdfSourceDescription.test(node);
    }

}
