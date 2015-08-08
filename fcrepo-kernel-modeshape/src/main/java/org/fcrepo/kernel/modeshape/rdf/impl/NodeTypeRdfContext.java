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
package org.fcrepo.kernel.modeshape.rdf.impl;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterators.forArray;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.Class;
import static com.hp.hpl.jena.vocabulary.RDFS.label;
import static com.hp.hpl.jena.vocabulary.RDFS.subClassOf;
import static org.fcrepo.kernel.api.utils.UncheckedFunction.uncheck;
import static org.fcrepo.kernel.modeshape.rdf.impl.mappings.ItemDefinitionToTriples.getResource;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Node;

import org.fcrepo.kernel.modeshape.rdf.impl.mappings.NodeDefinitionToTriples;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyDefinitionToTriples;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

/**
 * Assemble {@link com.hp.hpl.jena.graph.Triple}s derived from the {@link NodeType}s in a repository.
 *
 * @author cbeer
 */
public class NodeTypeRdfContext extends RdfStream {

    private static final Logger LOGGER = getLogger(NodeTypeRdfContext.class);

    private static final Predicate<ItemDefinition> isWildcardResidualDefinition = x -> x.getName().equals("*");

    /**
     * Convert the NodeTypeManager to an RDF stream, including both primary and
     * mixin node types.
     *
     * @param nodeTypeManager the node type manager
     * @throws RepositoryException if repository exception occurred
     */
    @SuppressWarnings("unchecked")
    public NodeTypeRdfContext(final NodeTypeManager nodeTypeManager) throws RepositoryException {
        super();

        concat(new NodeTypeRdfContext(nodeTypeManager.getPrimaryNodeTypes()));
        concat(new NodeTypeRdfContext(nodeTypeManager.getMixinNodeTypes()));
    }

    /**
     * Convert a NodeType iterator into an RDF stream
     *
     * @param nodeTypeIterator the node type iterator
     * @throws RepositoryException if repository exception occurred
     */
    public NodeTypeRdfContext(final Iterator<NodeType> nodeTypeIterator)
        throws RepositoryException {
        super();

        while (nodeTypeIterator.hasNext()) {
            concat(new NodeTypeRdfContext(nodeTypeIterator.next()));
        }
    }

    /**
     * Convert a NodeType into an RDF stream by capturing the supertypes, node
     * definitions, and property definitions of the type as RDFS triples.
     *
     * @param nodeType the node type
     * @throws RepositoryException if repository exception occurred
     */
    public NodeTypeRdfContext(final NodeType nodeType)
        throws RepositoryException {
        super();

        final Node nodeTypeResource = getResource(nodeType).asNode();
        final String nodeTypeName = nodeType.getName();

        LOGGER.trace("Adding triples for nodeType: {} with URI: {}",
                nodeTypeName, nodeTypeResource.getURI());

        concat(Collections2.transform(copyOf(nodeType.getDeclaredSupertypes()),
                    uncheck((final NodeType x) -> {
                        final Node supertypeNode = getResource(x).asNode();
                        LOGGER.trace(
                                "Adding triple for nodeType: {} with subclass: {}",
                                nodeTypeName, supertypeNode.getURI());
                        return create(nodeTypeResource,
                                subClassOf.asNode(), supertypeNode);
                    })::apply));

        concat(Iterators.concat(
            Iterators.transform(Iterators.filter(
                forArray(nodeType.getDeclaredChildNodeDefinitions()),
                isWildcardResidualDefinition.negate()::test),
                (new NodeDefinitionToTriples(nodeTypeResource))::apply)));

        concat(Iterators.concat(
            Iterators.transform(Iterators.filter(
                forArray(nodeType.getDeclaredPropertyDefinitions()),
                isWildcardResidualDefinition.negate()::test),
                (new PropertyDefinitionToTriples(nodeTypeResource))::apply)));

        concat(create(nodeTypeResource, type.asNode(), Class.asNode()), create(
                nodeTypeResource, label.asNode(), createLiteral(nodeTypeName)));
    }


}
