/*
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

import static java.util.stream.Stream.of;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static com.hp.hpl.jena.vocabulary.RDFS.Class;
import static com.hp.hpl.jena.vocabulary.RDFS.label;
import static com.hp.hpl.jena.vocabulary.RDFS.subClassOf;
import static org.fcrepo.kernel.modeshape.rdf.impl.mappings.ItemDefinitionToTriples.getResource;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.function.Predicate;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import org.fcrepo.kernel.modeshape.rdf.impl.mappings.NodeDefinitionToTriples;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyDefinitionToTriples;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

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
@Deprecated
public class NodeTypeRdfContext extends DefaultRdfStream {

    private static final Logger LOGGER = getLogger(NodeTypeRdfContext.class);

    private static final Predicate<ItemDefinition> isWildcardResidualDefinition = x -> x.getName().equals("*");

    /**
     * Convert the NodeTypeManager to an RDF stream, including both primary and
     * mixin node types.
     *
     * @param nodeTypeManager the node type manager
     * @throws RepositoryException if repository exception occurred
     */
    public NodeTypeRdfContext(final NodeTypeManager nodeTypeManager) throws RepositoryException {
        super(createURI("fedora:info/"));
        concat(getNodeTypes(nodeTypeManager).flatMap(getTriplesFromNodeType));
    }

    /**
     * Convert a stream of NodeTypes to an RDF stream, including both primary and
     * mixin node types.
     *
     * @param nodeTypes the node types
     */
    public NodeTypeRdfContext(final Stream<NodeType> nodeTypes) {
        super(createURI("fedora:info/"));
        concat(nodeTypes.flatMap(getTriplesFromNodeType));
    }

    /**
     * Convert a single NodeType to an RDF stream, including both primary and
     * mixin node types.
     *
     * @param nodeType the node type
     */
    public NodeTypeRdfContext(final NodeType nodeType) {
        super(createURI("fedora:info/"));
        concat(getTriplesFromNodeType.apply(nodeType));
    }

    @SuppressWarnings("unchecked")
    private static Stream<NodeType> getNodeTypes(final NodeTypeManager manager) throws RepositoryException {
         return Stream.concat(
                iteratorToStream(manager.getPrimaryNodeTypes()),
                iteratorToStream(manager.getMixinNodeTypes()));
    }

    private static Function<NodeType, Stream<Triple>> getTriplesFromNodeType = uncheck(nodeType -> {
        final Node nodeTypeResource = getResource(nodeType).asNode();
        final String nodeTypeName = nodeType.getName();

        return Stream.concat(
            Stream.concat(
                Arrays.stream(nodeType.getDeclaredSupertypes())
                    .map(uncheck((final NodeType x) -> {
                        final Node supertypeNode = getResource(x).asNode();
                        LOGGER.trace("Adding triple for nodeType: {} with subclass: {}", nodeTypeName,
                            supertypeNode.getURI());
                        return create(nodeTypeResource, subClassOf.asNode(), supertypeNode);
                    })),

                Arrays.stream(nodeType.getDeclaredChildNodeDefinitions())
                    .filter(isWildcardResidualDefinition.negate())
                    .flatMap((new NodeDefinitionToTriples(nodeTypeResource))::apply)),
            Stream.concat(
                Arrays.stream(nodeType.getDeclaredPropertyDefinitions())
                    .filter(isWildcardResidualDefinition.negate())
                    .flatMap((new PropertyDefinitionToTriples(nodeTypeResource))::apply),

                of(create(nodeTypeResource, type.asNode(), Class.asNode()),
                   create(nodeTypeResource, label.asNode(), createLiteral(nodeTypeName)))));
    });
}
