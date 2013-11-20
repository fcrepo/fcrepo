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
import static com.google.common.collect.Iterators.forArray;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;

/**
 * {@link RdfStream} that holds contexts related to a specific {@link Node}.
 *
 * @author ajs6f
 * @date Oct 10, 2013
 */
public class NodeRdfContext extends RdfStream {

    private final Node node;

    private final GraphSubjects graphSubjects;

    private final com.hp.hpl.jena.graph.Node subject;

    private final LowLevelStorageService lowLevelStorageService;

    private static final Logger LOGGER = getLogger(NodeRdfContext.class);

    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws RepositoryException
     */
    public NodeRdfContext(final Node node, final GraphSubjects graphSubjects,
        final LowLevelStorageService lowLevelStorageService)
        throws RepositoryException {
        super();
        this.node = node;
        this.graphSubjects = graphSubjects;
        this.subject = graphSubjects.getGraphSubject(node).asNode();

        // TODO fix GraphProperties to allow for LowLevelStorageServices to pass
        // through it
        // this is horribly ugly. LowLevelStorageServices are supposed to be
        // managed beans.
        // but the contract of GraphProperties isn't wide enough to pass one in,
        // so rather than
        // alter GraphProperties right now, I'm just spinning one on the fly.
        if (lowLevelStorageService == null) {
            this.lowLevelStorageService = new LowLevelStorageService();
            this.lowLevelStorageService.setRepository(node.getSession()
                    .getRepository());
        } else {
            this.lowLevelStorageService = lowLevelStorageService;
        }

        // add JCR mixins as rdf:type triples
        final Iterator<NodeType> nodeTypes = forArray(node.getMixinNodeTypes());
        concat(Iterators.transform(nodeTypes, nodetype2triple()));
    }

    /**
     * @return The {@link Node} in question
     */
    public Node node() {
        return node;
    }

    /**
     * @return local {@link GraphSubjects}
     */
    public GraphSubjects graphSubjects() {
        return graphSubjects;
    }

    /**
     * @return the RDF subject at the center of this context
     */
    public com.hp.hpl.jena.graph.Node subject() {
        return subject;
    }

    /**
     * @return the {@link LowLevelStorageService} in scope
     */
    public LowLevelStorageService lowLevelStorageService() {
        return lowLevelStorageService;
    }

    private final Function<NodeType, Triple> nodetype2triple() {
        return new Function<NodeType, Triple>() {

            @Override
            public Triple apply(final NodeType nodeType) {
                try {
                    final String fullTypeName = nodeType.getName();
                    LOGGER.trace("Translating JCR mixin name: {}", fullTypeName);
                    final String prefix = fullTypeName.split(":")[0];
                    final String typeName = fullTypeName.split(":")[1];
                    final String namespace = getJcrUri(prefix);
                    LOGGER.trace("with JCR namespace: {}", namespace);
                    final com.hp.hpl.jena.graph.Node rdfType =
                        createURI(getRDFNamespaceForJcrNamespace(namespace)
                                + typeName);
                    LOGGER.trace("into RDF resource: {}", rdfType);
                    return create(subject(), type.asNode(), rdfType);
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }

        };
    }

    private String getJcrUri(final String prefix) throws RepositoryException {
        return node().getSession().getWorkspace().getNamespaceRegistry()
                .getURI(prefix);
    }

}
