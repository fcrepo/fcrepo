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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
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
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws RepositoryException if repository exception occurred
     */
    public TypeRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        //include rdf:type for primaryType, mixins, and their supertypes
        concatRdfTypes();
    }

    private void concatRdfTypes() throws RepositoryException {
        final ImmutableList.Builder<NodeType> nodeTypesB = ImmutableList.<NodeType>builder();

        final NodeType primaryNodeType = resource().getNode().getPrimaryNodeType();

        if (primaryNodeType != null) {
            nodeTypesB.add(primaryNodeType);
        }

        try {
            final Set<NodeType> primarySupertypes = ImmutableSet.<NodeType>builder()
                    .add(primaryNodeType.getSupertypes()).build();
            nodeTypesB.addAll(primarySupertypes);
        } catch (NullPointerException e) {
            // ignore
        }

        final NodeType[] mixinNodeTypesArr = resource().getNode().getMixinNodeTypes();

        if (mixinNodeTypesArr != null) {
            final Set<NodeType> mixinNodeTypes = ImmutableSet.<NodeType>builder().add(mixinNodeTypesArr).build();
            nodeTypesB.addAll(mixinNodeTypes);

            final ImmutableSet.Builder<NodeType> mixinSupertypes = ImmutableSet.<NodeType>builder();
            for (final NodeType mixinNodeType : mixinNodeTypes) {
                mixinSupertypes.addAll(ImmutableSet.<NodeType>builder().add(mixinNodeType.getSupertypes()).build());
            }

            nodeTypesB.addAll(mixinSupertypes.build());
        }

        final ImmutableList<NodeType> nodeTypes = nodeTypesB.build();
        final Iterator<NodeType> nodeTypesIt = nodeTypes.iterator();

        concat(Iterators.transform(nodeTypesIt, nodetype2triple()));
    }

    private Function<NodeType, Triple> nodetype2triple() {
        return new Function<NodeType, Triple>() {

            @Override
            public Triple apply(final NodeType nodeType) {
                try {
                    final String name = nodeType.getName();
                    final String prefix = name.split(":")[0];
                    final String typeName = name.split(":")[1];
                    final String namespace = getJcrUri(prefix);
                    final com.hp.hpl.jena.graph.Node rdfType =
                            createURI(getRDFNamespaceForJcrNamespace(namespace)
                                    + typeName);
                    LOGGER.trace("Translating mixin: {} w/ namespace: {} into resource: {}", name, namespace, rdfType);
                    return create(subject(), type.asNode(), rdfType);
                } catch (final RepositoryException e) {
                    throw propagate(e);
                }
            }

        };
    }

    private String getJcrUri(final String prefix) throws RepositoryException {
        return resource().getNode().getSession().getWorkspace().getNamespaceRegistry()
                .getURI(prefix);
    }

}
