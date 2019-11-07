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
package org.fcrepo.kernel.impl.operations;

import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import java.util.List;
import java.util.stream.Collectors;

abstract public class AbstractRdfSourceOperationBuilder implements RdfSourceOperationBuilder {

    private static String rdfType = RDF_NAMESPACE + "type";

    String resourceId;

    Node resourceNode;

    /**
     * Constructor.
     * @param resourceId the internal identifier.
     */
    public AbstractRdfSourceOperationBuilder(final String resourceId) {
        this.resourceId = resourceId;
        this.resourceNode = ResourceFactory.createResource(this.resourceId).asNode();
    }

    /**
     * Perform validation on user RDF.
     * @param stream the incoming RDF stream.
     *
     * @return The same RDF stream.
     * @throws MalformedRdfException on server managed triple or restricted rdf:type
     */
    RdfStream validateIncomingRdf(final RdfStream stream) {
        final List<Triple> triples = stream.collect(Collectors.toList());
        final Node topic = stream.topic();
        checkForLdpTypes(new DefaultRdfStream(topic, triples.stream()));
        return new DefaultRdfStream(topic, triples.stream());
    }

    /**
     * Looks through an RdfStream for rdf:types in the LDP namespace or server managed predicates.
     *
     * @param stream The RDF stream to check.
     */
    void checkForLdpTypes(final RdfStream stream) {
        if (stream.anyMatch(p ->
                // predicate is rdf:type and object starts with LDP namespace.
                (p.getPredicate().hasURI(rdfType) && p.getObject().toString().startsWith(LDP_NAMESPACE)) ||
                // predicate is server managed.
                isManagedPredicate.test(ResourceFactory.createProperty(p.getPredicate().toString())))
        ) {
            throw new MalformedRdfException("RDF contains a server managed triple or restricted rdf:type");
        }
    }
}
