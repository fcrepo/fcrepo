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

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSource;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.fcrepo.kernel.api.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.api.utils.UncheckedFunction.uncheck;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
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
        if (resource() instanceof NonRdfSource) {
            // gather versionability info from the parent
            if (resource().getNode().getParent().isNodeType("mix:versionable")) {
                concat(create(subject(), type.asNode(), createURI(MIX_NAMESPACE + "versionable")));
            }
        }
    }

    private void concatRdfTypes() throws RepositoryException {
        final List<NodeType> nodeTypes = new ArrayList<>();
        final NodeType primaryNodeType = resource().getNode().getPrimaryNodeType();
        nodeTypes.add(primaryNodeType);
        nodeTypes.addAll(asList(primaryNodeType.getSupertypes()));
        final NodeType[] mixinNodeTypesArr = resource().getNode().getMixinNodeTypes();
        stream(mixinNodeTypesArr).forEach(nodeTypes::add);
        stream(mixinNodeTypesArr).map(NodeType::getSupertypes).flatMap(Arrays::stream).forEach(nodeTypes::add);
        concat(nodeTypes.stream().map(nodetype2triple).iterator());
    }

    private final Function<NodeType, Triple> nodetype2triple = uncheck(nodeType -> {
        final String name = nodeType.getName();
        final String prefix = name.split(":")[0];
        final String typeName = name.split(":")[1];
        final String namespace = getJcrUri(prefix);
        final com.hp.hpl.jena.graph.Node rdfType = createURI(getRDFNamespaceForJcrNamespace(namespace) + typeName);
        LOGGER.trace("Translating mixin: {} w/ namespace: {} into resource: {}", name, namespace, rdfType);
        return create(subject(), type.asNode(), rdfType);
    });

    private String getJcrUri(final String prefix) throws RepositoryException {
        return session().getWorkspace().getNamespaceRegistry().getURI(prefix);
    }
}
