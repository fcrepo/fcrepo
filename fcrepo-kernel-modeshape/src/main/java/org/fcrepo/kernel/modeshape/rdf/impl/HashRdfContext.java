/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.ManagedRdf.isManagedTriple;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/9/14
 */
public class HashRdfContext extends NodeRdfContext {

    private static final Predicate<Triple> IS_MANAGED_TYPE = t -> t.getPredicate().equals(type.asNode()) &&
            isManagedNamespace.test(t.getObject().getNameSpace());

    private static final Predicate<Triple> IS_MANAGED_TRIPLE = IS_MANAGED_TYPE.or(isManagedTriple);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public HashRdfContext(final FedoraResource resource,
                          final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        concat(getNodeStream(resource)
                .flatMap(n -> nodeConverter.convert(n).getTriples(idTranslator, PROPERTIES))
                .filter(IS_MANAGED_TRIPLE.negate()));
    }

    @SuppressWarnings("unchecked")
    private static Stream<Node> getNodeStream(final FedoraResource resource) throws RepositoryException {
        final Node node = getJcrNode(resource);
        if (node.hasNode("#")) {
            return iteratorToStream(node.getNode("#").getNodes());
        }
        return Stream.empty();
    }
}
