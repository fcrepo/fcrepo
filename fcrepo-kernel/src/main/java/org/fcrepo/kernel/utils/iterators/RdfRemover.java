/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.utils.iterators;

import static org.fcrepo.kernel.utils.NodePropertiesTools.removeNodeProperty;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Consumes an {@link RdfStream} by removing its contents from the
 * JCR.
 *
 * @see {@link RdfAdder} for contrast
 * @author ajs6f
 * @date Oct 24, 2013
 */
public class RdfRemover extends RdfPersister {

    private static final Logger LOGGER = getLogger(RdfRemover.class);

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public RdfRemover(final GraphSubjects graphSubjects, final Session session,
        final RdfStream stream) {
        super(graphSubjects, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource,
        final Node subjectNode) throws RepositoryException {
        final String namespacePrefix;
        try {
            namespacePrefix =
                session.getNamespacePrefix(mixinResource.getNameSpace());
        } catch (final NamespaceException e) {
            throw new MalformedRdfException(
                    "Unable to resolve registered namespace for resource "
                            + mixinResource.toString());
        }
        final String mixinName =
            namespacePrefix + ":" + mixinResource.getLocalName();
        if (session.getWorkspace().getNodeTypeManager().hasNodeType(mixinName)) {
            LOGGER.debug("Removing mixin: {} from node: {}.", mixinName,
                    subjectNode.getPath());
            subjectNode.removeMixin(mixinName);
        }
    }

    @Override
    protected void operateOnOneValueOfProperty(final Node n, final String p,
        final Value v) throws RepositoryException {
        LOGGER.debug("Adding property: {} with value: {} to node: {}.", p, v, n
                .getPath());
        removeNodeProperty(n, p, v);
    }
}
