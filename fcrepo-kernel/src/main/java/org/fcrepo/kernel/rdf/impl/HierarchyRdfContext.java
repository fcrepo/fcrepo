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

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.RdfLexicon.INLINED_RESOURCE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isInternalNode;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.NodeRdfContext;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.iterators.NodeIterator;
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

    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws RepositoryException
     */
    public HierarchyRdfContext(final javax.jcr.Node node,
            final GraphSubjects graphSubjects, final LowLevelStorageService lowLevelStorageService) throws RepositoryException {
        super(node, graphSubjects, lowLevelStorageService);
        if (node.getDepth() > 0) {
            concat(parentContext());

        }
        if (node.hasNodes()) {
            concat(childrenContext());
        }
    }

    private Iterator<Triple> parentContext() throws RepositoryException {
        final javax.jcr.Node parentNode = node().getParent();
        final Node parentNodeSubject =
            graphSubjects().getGraphSubject(parentNode).asNode();
        return new PropertiesRdfContext(parentNode, graphSubjects(), lowLevelStorageService())
                .concat(Iterators
                        .forArray(new Triple[] {
                                create(subject(), HAS_PARENT.asNode(),
                                        parentNodeSubject),
                                create(parentNodeSubject, HAS_CHILD.asNode(),
                                        subject()),
                                create(graphSubjects().getContext().asNode(),
                                        INLINED_RESOURCE.asNode(),
                                        parentNodeSubject)

                        }));

    }

    private Iterator<Triple> childrenContext() throws RepositoryException {
        Iterators.filter(new NodeIterator(node().getNodes()), nastyChildren);

        return null;
    }

    /**
     * Children for whom we will not generate triples.
     */
    private static Predicate<javax.jcr.Node> nastyChildren =
        new Predicate<javax.jcr.Node>() {

            @Override
            public boolean apply(final javax.jcr.Node n) {
                try {
                    return (isInternalNode.apply(n) || n.getName().equals(
                            JCR_CONTENT));
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }
        };

}
