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

package org.fcrepo.kernel.rdf.impl;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.lang.Boolean.TRUE;
import static org.fcrepo.kernel.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.RdfLexicon.INLINED_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.MEMBERS_INLINED;
import static org.fcrepo.kernel.RdfLexicon.PAGE;
import static org.fcrepo.kernel.RdfLexicon.PAGE_OF;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isInternalNode;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.iterators.NodeIterator;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * An {@link RdfContext} that contains information about the JCR hierarchy
 * around a given node.
 *
 * @author ajs6f
 * @date Oct 10, 2013
 */
public class HierarchyRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(HierarchyRdfContext.class);


    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws RepositoryException
     */
    public HierarchyRdfContext(final javax.jcr.Node node,
            final IdentifierTranslator graphSubjects,
            final LowLevelStorageService lowLevelStorageService)
        throws RepositoryException {

        super(node, graphSubjects, lowLevelStorageService);
        if (node.getDepth() > 0) {
            LOGGER.trace("Determined that this node has a parent.");
            concat(parentContext());
        }
        final Node pageContext = graphSubjects.getContext().asNode();
        concat(new Triple[] {
                create(pageContext, type.asNode(), PAGE.asNode()),
                create(pageContext, PAGE_OF.asNode(), subject())});

        if (JcrRdfTools.isContainer(node)) {
            LOGGER.trace("Determined that this node is a container.");
            concat(containerContext(pageContext));
        } else {
            LOGGER.trace("Determined that this node is not a container.");
        }

        if (node.hasNodes()) {
            LOGGER.trace("Found children of this node.");
            concat(childrenContext(pageContext));
        }
    }

    private Triple[] containerContext(final Node pageContext) {
        return new Triple[] {
                create(pageContext, MEMBERS_INLINED.asNode(),
                        createLiteral(TRUE.toString())),
                create(subject(), type.asNode(), CONTAINER.asNode()),
                create(subject(), type.asNode(), DIRECT_CONTAINER.asNode()),
                create(subject(), MEMBERSHIP_RESOURCE.asNode(), subject()),
                create(subject(), HAS_MEMBER_RELATION.asNode(), HAS_CHILD
                        .asNode())};
    }

    private Iterator<Triple> parentContext() throws RepositoryException {
        final javax.jcr.Node parentNode = node().getParent();
        final Node parentNodeSubject = graphSubjects().getSubject(parentNode.getPath()).asNode();
        return new PropertiesRdfContext(parentNode, graphSubjects(), lowLevelStorageService())
                .concat(new Triple[] {
                                create(subject(), HAS_PARENT.asNode(),
                                        parentNodeSubject),
                                create(parentNodeSubject, HAS_CHILD.asNode(),
                                        subject()),
                                create(graphSubjects().getContext().asNode(),
                                        INLINED_RESOURCE.asNode(),
                                        parentNodeSubject)

                });

    }

    private Iterator<Triple> childrenContext(final Node pageContext) throws RepositoryException {

        final Iterator<javax.jcr.Node> niceChildren =
            Iterators.filter(new NodeIterator(node().getNodes()), not(nastyChildren));

        return Iterators.concat(
                Iterators.transform(niceChildren, child2triples(pageContext)));
    }

    private Function<javax.jcr.Node, Iterator<Triple>> child2triples(
            final Node pageContext) {
        return new Function<javax.jcr.Node, Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final javax.jcr.Node child) {
                try {
                    final Node childSubject = graphSubjects().getSubject(child.getPath()).asNode();
                    LOGGER.trace("Creating triples for child node: {}", child);
                    return new PropertiesRdfContext(child, graphSubjects(),
                        lowLevelStorageService()).concat(new Triple[] {
                            create(pageContext, INLINED_RESOURCE.asNode(),
                                    childSubject),
                            create(childSubject, HAS_PARENT.asNode(),
                                    subject()),
                            create(subject(), HAS_CHILD.asNode(),
                                    childSubject)});
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
