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

import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.RdfLexicon.*;

import java.util.Collection;
import java.util.function.Predicate;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;

/**
 * Helper methods for determining if resources contain all required server managed triples.
 *
 * @author bbpennel
 */
public class RdfStreamRequiredPropertiesUtil {

    private static abstract class RequiredNodePredicate implements Predicate<Triple> {
        protected Node node;

        public Node getNode() {
            return node;
        }
    }

    private static class RequiredPropertyPredicate extends RequiredNodePredicate {

        public RequiredPropertyPredicate(final Property property) {
            node = property.asNode();
        }

        @Override
        public boolean test(final Triple t) {
            return t.getPredicate().equals(node);
        }
    }

    private static class RequiredTypePredicate extends RequiredNodePredicate {

        public RequiredTypePredicate(final Resource resource) {
            node = resource.asNode();
        }

        @Override
        public boolean test(final Triple t) {
            return t.getPredicate().equals(RDF.type.asNode()) && t.getObject().equals(node);
        }
    }

    private final static Collection<Property> REQUIRED_PROPERTIES = asList(
            CREATED_BY, CREATED_DATE, LAST_MODIFIED_DATE, LAST_MODIFIED_BY);

    private final static Collection<Resource> CONTAINER_TYPES = asList(
            RDF_SOURCE, CONTAINER, FEDORA_CONTAINER, FEDORA_RESOURCE);

    private final static Collection<Resource> BINARY_TYPES = asList(
            NON_RDF_SOURCE, FEDORA_BINARY, FEDORA_RESOURCE);

    private final static Collection<RequiredNodePredicate> CONTAINER_PROPERTIES = asList(
            new RequiredPropertyPredicate(CREATED_BY),
            new RequiredPropertyPredicate(CREATED_DATE),
            new RequiredPropertyPredicate(LAST_MODIFIED_DATE),
            new RequiredPropertyPredicate(LAST_MODIFIED_BY),
            new RequiredTypePredicate(RDF_SOURCE),
            new RequiredTypePredicate(CONTAINER),
            new RequiredTypePredicate(FEDORA_CONTAINER),
            new RequiredTypePredicate(FEDORA_RESOURCE));

    private final static Collection<RequiredNodePredicate> BINARY_DESCRIPTION_PROPERTIES = asList(
            new RequiredPropertyPredicate(CREATED_BY),
            new RequiredPropertyPredicate(CREATED_DATE),
            new RequiredPropertyPredicate(LAST_MODIFIED_DATE),
            new RequiredPropertyPredicate(LAST_MODIFIED_BY),
            new RequiredTypePredicate(NON_RDF_SOURCE),
            new RequiredTypePredicate(FEDORA_BINARY),
            new RequiredTypePredicate(FEDORA_RESOURCE));

    public static void assertContainsRequiredContainerTriples(final Model model)
            throws ConstraintViolationException {
        for (final Property property : REQUIRED_PROPERTIES) {
            if (!model.contains(null, property, (RDFNode) null)) {
                throw new ConstraintViolationException("Missing required property " +
                        property.getURI());
            }
        }
        for (final Resource type : CONTAINER_TYPES) {
            if (!model.contains(null, RDF.type, type)) {
                throw new ConstraintViolationException("Missing required type " +
                        type.getURI());
            }
        }
    }

    /**
     * Throws a ConstraintViolationException if the rdfStream does not contain all required server managed triples for
     * a container.
     *
     * @param rdfStream rdf stream
     * @throws ConstraintViolationException
     */
    public static void assertContainsRequiredContainerTriples(final RdfStream rdfStream)
            throws ConstraintViolationException {
        assertContainsRequireTriples(rdfStream, CONTAINER_PROPERTIES);
    }

    /**
     * Throws a ConstraintViolationException if the rdfStream does not contain all required server managed triples for
     * a binary.
     *
     * @param rdfStream rdf stream of a binary description
     * @throws ConstraintViolationException
     */
    public static void assertContainsRequiredBinaryTriples(final RdfStream rdfStream)
            throws ConstraintViolationException {
        assertContainsRequireTriples(rdfStream, BINARY_DESCRIPTION_PROPERTIES);
    }

    private static void assertContainsRequireTriples(final RdfStream rdfStream,
            final Collection<RequiredNodePredicate> requiredPredicates) throws ConstraintViolationException {
        for (final RequiredNodePredicate required : requiredPredicates) {
            if (!rdfStream.anyMatch(required)) {
                if (required instanceof RequiredPropertyPredicate) {
                    throw new ConstraintViolationException("Missing required property " +
                            required.getNode().getURI());
                } else {
                    throw new ConstraintViolationException("Missing required type " + required.getNode().getURI());
                }
            }
        }
    }
}
