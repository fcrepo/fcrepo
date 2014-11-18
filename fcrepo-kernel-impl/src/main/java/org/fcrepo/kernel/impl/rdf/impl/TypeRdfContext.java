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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.ImmutableSet.builder;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.impl.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/1/14
 */
public class TypeRdfContext extends NodeRdfContext {
    private static final Logger LOGGER = getLogger(TypeRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */
    public TypeRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        //include rdf:type for primaryType, mixins, and their supertypes
        concatRdfTypes();
    }

    private void concatRdfTypes() throws RepositoryException {
        final ImmutableSet.Builder<NodeType> nodeTypesB = builder();

        final NodeType primaryNodeType = resource().getNode().getPrimaryNodeType();
        nodeTypesB.add(primaryNodeType);

        final Set<NodeType> primarySupertypes = setOf(primaryNodeType.getSupertypes());
        nodeTypesB.addAll(primarySupertypes);


        final NodeType[] mixinNodeTypesArr = resource().getNode().getMixinNodeTypes();


        final Set<NodeType> mixinNodeTypes = setOf(mixinNodeTypesArr);
        nodeTypesB.addAll(mixinNodeTypes);

        final ImmutableSet.Builder<NodeType> mixinSupertypes = builder();
        for (final NodeType mixinNodeType : mixinNodeTypes) {
            mixinSupertypes.addAll(asList(mixinNodeType.getSupertypes()));
        }

        nodeTypesB.addAll(mixinSupertypes.build());

        final Set<NodeType> nodeTypes = nodeTypesB.build();

        concat(Iterables.transform(nodeTypes, nodetype2triple()).iterator());
    }

    private Function<NodeType, Triple> nodetype2triple() {
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

    private static Set<NodeType> setOf(final NodeType[] types) {
        return types == null ? Collections.<NodeType>emptySet() : copyOf(types);
    }

    private String getJcrUri(final String prefix) throws RepositoryException {
        return resource().getNode().getSession().getWorkspace().getNamespaceRegistry()
                .getURI(prefix);
    }

}
