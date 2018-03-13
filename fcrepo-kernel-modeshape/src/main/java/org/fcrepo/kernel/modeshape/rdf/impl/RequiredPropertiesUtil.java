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
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;

import java.util.Collection;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;

/**
 * Helper methods for determining if resources contain all required server managed triples.
 *
 * @author bbpennel
 */
public class RequiredPropertiesUtil {

    private RequiredPropertiesUtil() {
    }

    private final static Collection<Property> REQUIRED_PROPERTIES = asList(
            CREATED_BY, CREATED_DATE, LAST_MODIFIED_DATE, LAST_MODIFIED_BY);

    private final static Collection<Resource> CONTAINER_TYPES = asList(
            RDF_SOURCE, CONTAINER, FEDORA_CONTAINER, FEDORA_RESOURCE);

    private final static Collection<Resource> BINARY_TYPES = asList(
            NON_RDF_SOURCE, FEDORA_BINARY, FEDORA_RESOURCE);

    /**
     * Throws a ConstraintViolationException if the model does not contain all required server managed triples for a
     * container.
     *
     * @param model rdf to validate
     * @throws ConstraintViolationException
     */
    public static void assertRequiredContainerTriples(final Model model)
            throws ConstraintViolationException {
        assertContainsRequiredProperties(model, REQUIRED_PROPERTIES);
        assertContainsRequiredTypes(model, CONTAINER_TYPES);
    }

    /**
     * Throws a ConstraintViolationException if the model does not contain all required server managed triples for a
     * binary description.
     *
     * @param model rdf to validate
     * @throws ConstraintViolationException
     */
    public static void assertRequiredBinaryTriples(final Model model)
            throws ConstraintViolationException {
        assertContainsRequiredProperties(model, REQUIRED_PROPERTIES);
        assertContainsRequiredTypes(model, BINARY_TYPES);
    }

    private static void assertContainsRequiredProperties(final Model model, final Collection<Property> properties) {
        for (final Property property : properties) {
            if (!model.contains(null, property, (RDFNode) null)) {
                throw new ConstraintViolationException("Missing required property " +
                        property.getURI());
            }
        }
    }

    private static void assertContainsRequiredTypes(final Model model, final Collection<Resource> types) {
        for (final Resource type : types) {
            if (!model.contains(null, RDF.type, type)) {
                throw new ConstraintViolationException("Missing required type " +
                        type.getURI());
            }
        }
    }
}
