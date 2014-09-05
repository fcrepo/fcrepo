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
package org.fcrepo.kernel.impl.utils.iterators;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.fcrepo.kernel.impl.utils.NodePropertiesTools;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Consumes an {@link org.fcrepo.kernel.utils.iterators.RdfStream} by removing its contents from the
 * JCR.
 *
 * @see RdfAdder
 * @author ajs6f
 * @since Oct 24, 2013
 */
public class RdfRemover extends PersistingRdfStreamConsumer {

    private static final Logger LOGGER = getLogger(RdfRemover.class);
    private final NodePropertiesTools propertiesTools = new NodePropertiesTools();

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public RdfRemover(final IdentifierTranslator graphSubjects, final Session session,
        final RdfStream stream) {
        super(graphSubjects, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource,
        final Node subjectNode) throws RepositoryException {

        final String mixinName = getPropertyNameFromPredicate(subjectNode, mixinResource);
        if (sessionHasType(session(), mixinName)) {
            LOGGER.debug("Removing mixin: {} from node: {}.", mixinName,
                    subjectNode.getPath());

            if (subjectNode.getPrimaryNodeType().isNodeType(mixinName)) {
                LOGGER.debug("Unable to remove primary type from node");
                return;
            }

            try {
                subjectNode.removeMixin(mixinName);
            } catch (final NoSuchNodeTypeException e) {
                LOGGER.debug("which that node turned out not to have.");
                LOGGER.trace("Backtrace: ", e);
            }
        }
    }

    @Override
    protected void operateOnProperty(final Statement t, final Node n)
        throws RepositoryException {
        LOGGER.debug("Trying to remove property from triple: {} on node: {}.", t, n
                .getPath());

        final String propertyName =
            getPropertyNameFromPredicate(n, t.getPredicate());
        if (n.hasProperty(propertyName)) {
            final Value v = createValue(n, t, propertyName);
            propertiesTools.removeNodeProperty(idTranslator(), n, propertyName, v);
        }
    }
}
