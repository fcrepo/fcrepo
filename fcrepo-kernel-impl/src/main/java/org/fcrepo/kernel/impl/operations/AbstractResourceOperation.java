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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CREATED;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIED;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract operation for interacting with a resource
 *
 * @author bbpennel
 */
public abstract class AbstractResourceOperation implements ResourceOperation {

    /**
     * The internal Fedora ID.
     */
    private final String rescId;

    /**
     * The server managed triples.
     */
    private RdfStream serverManagedProperties;

    /**
     * SMTs as a list during generation.
     */
    private List<Triple> serverManagedAsList;

    protected AbstractResourceOperation(final String rescId) {
        this.rescId = rescId;
        populateServerManagedTriples();
        this.serverManagedProperties = new DefaultRdfStream(asNode(rescId), this.serverManagedAsList.stream());
    }

    /**
     * Populate server managed properties.
     * Override in subclasses to add additional triples, always call super() first;
     */
    void populateServerManagedTriples() {
        this.serverManagedAsList = new ArrayList<>();
        final ZonedDateTime now = ZonedDateTime.now();
        if (this.getType() == ResourceOperationType.CREATE) {
            this.serverManagedAsList.add(addTriple(asNode(FEDORA_CREATED),
                    asLiteral(now.format(DateTimeFormatter.RFC_1123_DATE_TIME), XSDDatatype.XSDdateTime)));
            // TODO: get current user.
            // this.serverManagedAsList.add(addTriple(asNode(FEDORA_CREATEDBY), asLiteral(user)));
        }
        serverManagedAsList.add(addTriple(asNode(FEDORA_LASTMODIFIED),
                asLiteral(now.format(DateTimeFormatter.RFC_1123_DATE_TIME), XSDDatatype.XSDdateTime)));
        // TODO: get current user.
        // serverManagedAsList.add(addTriple(asNode(FEDORA_LASTMODIFIEDBY), asLiteral(user)));
    }

    @Override
    public String getResourceId() {
        return rescId;
    }

    @Override
    public RdfStream getServerManagedProperties() {
        return serverManagedProperties;
    }

    /**
     * Set the server managed properties for the resource
     *
     * @param serverManagedProperties stream of properties
     */
    public void setServerManagedProperties(final RdfStream serverManagedProperties) {
        this.serverManagedProperties = serverManagedProperties;
    }

    /**
     * Add a triple for this resource.
     * @param predicate the predicate.
     * @param object the resource object.
     * @return the triple.
     */
    Triple addTriple(final Node predicate, final Node object) {
        return new Triple(asNode(this.rescId), predicate, object);
    }

    /**
     * Add a triple for this resource.
     * @param predicate the predicate.
     * @param object the literal object.
     * @return the triple.
     */
    Triple addTriple(final Node predicate, final Literal object) {
        return new Triple(asNode(this.rescId), predicate, object.asNode());
    }

    /**
     * Create a node from a string.
     * @param resource the string.
     * @return the node.
     */
    Node asNode(final String resource) {
        return ResourceFactory.createResource(resource).asNode();
    }

    /**
     * Create a literal from a string.
     * @param literal the string.
     * @return the node of the literal.
     */
    Node asLiteral(final String literal) {
        return ResourceFactory.createPlainLiteral(literal).asNode();
    }

    /**
     * Create a typed literal from a string.
     * @param literal the string.
     * @param type the type of literal.
     * @return the node of the literal.
     */
    Node asLiteral(final String literal, final RDFDatatype type) {
        return ResourceFactory.createTypedLiteral(literal, type).asNode();
    }
}
