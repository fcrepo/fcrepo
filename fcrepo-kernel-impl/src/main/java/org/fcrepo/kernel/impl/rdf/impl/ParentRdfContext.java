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
package org.fcrepo.kernel.impl.rdf.impl;

import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Iterator;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.HAS_PARENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/16/14
 */
public class ParentRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ParentRdfContext.class);

    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws javax.jcr.RepositoryException
     */
    public ParentRdfContext(final Node node, final IdentifierTranslator graphSubjects) throws RepositoryException {
        super(node, graphSubjects);

        if (node.getDepth() > 0) {
            LOGGER.trace("Determined that this node has a parent.");
            concat(parentContext());
        }
    }

    private Iterator<Triple> parentContext() throws RepositoryException {
        final javax.jcr.Node parentNode = node().getParent();
        final com.hp.hpl.jena.graph.Node parentNodeSubject = graphSubjects().getSubject(parentNode.getPath()).asNode();

        final RdfStream parentStream = new RdfStream();

        parentStream.concat(create(subject(), HAS_PARENT.asNode(), parentNodeSubject));

        return parentStream;
    }
}
