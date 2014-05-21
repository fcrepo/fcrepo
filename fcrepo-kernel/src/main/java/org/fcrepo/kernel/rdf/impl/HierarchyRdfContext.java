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
package org.fcrepo.kernel.rdf.impl;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isInternalNode;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * An {@link NodeRdfContext} that contains information about the JCR hierarchy
 * around a given node.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class HierarchyRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(HierarchyRdfContext.class);
    private final HierarchyRdfContextOptions options;


    /**
     * Default constructor.
     *
     *
     *
     * @param node
     * @param graphSubjects
     * @param options
     * @throws RepositoryException
     */
    public HierarchyRdfContext(final javax.jcr.Node node,
                               final IdentifierTranslator graphSubjects,
                               final HierarchyRdfContextOptions options)
        throws RepositoryException {

        super(node, graphSubjects);
        this.options = options;

        if (JcrRdfTools.isContainer(node)) {
            LOGGER.trace("Determined that this node is a container.");
            concat(containerContext());
        } else {
            LOGGER.trace("Determined that this node is not a container.");
        }

        if (node.getDepth() > 0) {
            LOGGER.trace("Determined that this node has a parent.");
            concat(parentContext());
        }

        if ((options.membershipEnabled() || options.containmentEnabled()) && node.hasNodes()) {
            LOGGER.trace("Found children of this node.");
            concat(childrenContext());
        }
    }

    private Triple[] containerContext() {
        return new Triple[] {
                create(subject(), type.asNode(), CONTAINER.asNode()),
                create(subject(), type.asNode(), DIRECT_CONTAINER.asNode()),
                create(subject(), MEMBERSHIP_RESOURCE.asNode(), subject()),
                create(subject(), HAS_MEMBER_RELATION.asNode(), HAS_CHILD
                        .asNode())};
    }

    private Iterator<Triple> parentContext() throws RepositoryException {
        final javax.jcr.Node parentNode = node().getParent();
        final Node parentNodeSubject = graphSubjects().getSubject(parentNode.getPath()).asNode();

        final RdfStream parentStream = new RdfStream();

        parentStream.concat(create(subject(), HAS_PARENT.asNode(), parentNodeSubject));

        return parentStream;
    }

    private Iterator<Triple> childrenContext() throws RepositoryException {
        final Iterator<javax.jcr.Node> niceChildren = FedoraResourceImpl.getChildren(node(), graphSubjects());
            //Iterators.filter(new NodeIterator(node().getNodes()), not(nastyChildren));

        final Iterator<javax.jcr.Node> salientChildren;

        if (options.hasOffset()) {
            final int offset = options.getOffset();
            Iterators.advance(niceChildren, offset);
        }

        if (options.hasLimit()) {
            salientChildren = Iterators.limit(niceChildren, options.getLimit());
        } else {
            salientChildren = niceChildren;
        }

        return Iterators.concat(Iterators.transform(salientChildren, child2triples()));
    }

    private Function<javax.jcr.Node, Iterator<Triple>> child2triples() {
        return new Function<javax.jcr.Node, Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final javax.jcr.Node child) {
                try {
                    final Node childSubject = graphSubjects().getSubject(child.getPath()).asNode();
                    LOGGER.trace("Creating triples for child node: {}", child);
                    final RdfStream childStream = new RdfStream();


                    if (options.membershipEnabled()) {
                        childStream.concat(create(subject(), HAS_CHILD.asNode(), childSubject));
                    }

                    if (options.containmentEnabled()) {

                        childStream.concat(
                            new PropertiesRdfContext(child, graphSubjects())
                        );
                        childStream.concat(create(childSubject, HAS_PARENT.asNode(), subject()),
                                           create(subject(), CONTAINS.asNode(), childSubject));
                    }

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
    private static Predicate<javax.jcr.Node> nastyChildren =
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
