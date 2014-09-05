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
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.impl.utils.NodePropertiesTools;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Consumes an {@link org.fcrepo.kernel.utils.iterators.RdfStream} by adding its contents to the
 * JCR.
 *
 * @see RdfRemover
 * @author ajs6f
 * @since Oct 24, 2013
 */
public class RdfAdder extends PersistingRdfStreamConsumer {

    private static final Logger LOGGER = getLogger(RdfAdder.class);

    private final NodePropertiesTools propertiesTools = new NodePropertiesTools();

    /**
     * Ordinary constructor.
     *
     * @param graphSubjects
     * @param session
     * @param stream
     */
    public RdfAdder(final IdentifierTranslator graphSubjects, final Session session,
        final RdfStream stream) {
        super(graphSubjects, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource,
        final Node subjectNode) throws RepositoryException {

        final String mixinName = getPropertyNameFromPredicate(subjectNode, mixinResource);
        if (!sessionHasType(session(), mixinName)) {
            final NodeTypeManager mgr = session().getWorkspace().getNodeTypeManager();
            final NodeTypeTemplate type = mgr.createNodeTypeTemplate();
            type.setName(mixinName);
            type.setMixin(true);
            type.setQueryable(true);
            mgr.registerNodeType(type, false);
        }

        if (subjectNode.isNodeType(mixinName)) {
            LOGGER.trace("Subject {} is already a {}; skipping", subjectNode, mixinName);
            return;
        }

        if (subjectNode.canAddMixin(mixinName)) {
            LOGGER.debug("Adding mixin: {} to node: {}.", mixinName, subjectNode.getPath());
            subjectNode.addMixin(mixinName);
        } else {
            throw new MalformedRdfException("Could not persist triple containing type assertion: "
                                                    + mixinResource.toString()
                                                    + " because no such mixin/type can be added to this node: "
                                                    + subjectNode.getPath() + "!");
        }
    }


    @Override
    protected void operateOnProperty(final Statement t, final Node n) throws RepositoryException {
        LOGGER.debug("Adding property from triple: {} to node: {}.", t, n
                .getPath());

        final String propertyName =
            getPropertyNameFromPredicate(n, t.getPredicate());
        final Value v = createValue(n, t, propertyName);
        propertiesTools.appendOrReplaceNodeProperty(idTranslator(), n, propertyName, v);
    }
}
