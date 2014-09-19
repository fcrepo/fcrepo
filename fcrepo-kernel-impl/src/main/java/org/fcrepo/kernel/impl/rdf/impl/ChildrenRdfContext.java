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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.utils.iterators.NodeIterator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Iterator;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalNode;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/16/14
 */
public class ChildrenRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws javax.jcr.RepositoryException
     */
    public ChildrenRdfContext(final Node node, final IdentifierTranslator graphSubjects) throws RepositoryException {
        super(node, graphSubjects);

        if (node.hasNodes()) {
            LOGGER.trace("Found children of this node.");
            concat(childrenContext());
        }
    }


    private Iterator<Triple> childrenContext() throws RepositoryException {

        final Iterator<javax.jcr.Node> niceChildren =
                Iterators.filter(new NodeIterator(node().getNodes()), not(nastyChildren));

        return Iterators.concat(Iterators.transform(niceChildren, child2triples()));
    }

    private Function<Node, Iterator<Triple>> child2triples() {
        return new Function<javax.jcr.Node, Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final javax.jcr.Node child) {
                try {
                    final com.hp.hpl.jena.graph.Node childSubject
                            = graphSubjects().getSubject(child.getPath()).asNode();
                    LOGGER.trace("Creating triples for child node: {}", child);
                    final RdfStream childStream = new RdfStream();

                    childStream.concat(create(subject(), CONTAINS.asNode(), childSubject));
                    childStream.concat(create(subject(), HAS_CHILD.asNode(), childSubject));

                    return childStream;
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }
        };
    }

    /**
     * Children for whom we will not generate triples.
     */
    private static Predicate<Node> nastyChildren =
            new Predicate<javax.jcr.Node>() {

                @Override
                public boolean apply(final javax.jcr.Node n) {
                    LOGGER.trace("Testing child node {}", n);
                    try {
                        return (isInternalNode.apply(n) || n.getName().equals(
                                JCR_CONTENT));
                    } catch (final RepositoryException e) {
                        throw propagate(e);
                    }
                }
            };

}
