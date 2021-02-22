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
package org.fcrepo.kernel.api.utils;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.vocabulary.RDF.Init.type;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.RdfLexicon.isRelaxablePredicate;
import static org.fcrepo.kernel.api.RdfLexicon.restrictedType;

import java.util.Calendar;

import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RelaxableServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * Some server managed triples can have the prohibition on user-management overridden.  While
 * the server still updates them implicitly, it may be possible in some cases for a user
 * request to override them.
 *
 * @author Mike Durbin
 * @author whikloj
 */
public class RelaxedPropertiesHelper {

    /**
     * Gets the created date (if any) that was included in the statements.
     * @param resource the resource we are looking for properties of
     * @return the date that should be set for the CREATED_DATE or null if it should be
     *         untouched
     */
    public static Calendar getCreatedDate(final Resource resource) {
        final Iterable<Statement> iter = getIterable(resource, CREATED_DATE);
        return extractSingleCalendarValue(iter, CREATED_DATE);
    }

    /**
     * Gets the created by user (if any) that is included within the statements.
     * @param resource the resource we are looking for properties of
     * @return the string that should be set for the CREATED_BY or null if it should be
     *         untouched
     */
    public static String getCreatedBy(final Resource resource) {
        final Iterable<Statement> iter = getIterable(resource, CREATED_BY);
        return extractSingleStringValue(iter, CREATED_BY);
    }

    /**
     * Gets the modified date (if any) that was included within the statements.
     * @param resource the resource we are looking for properties of
     * @return the date that should be set for the LAST_MODIFIED_DATE or null if it should be
     *         untouched
     */
    public static Calendar getModifiedDate(final Resource resource) {
        final Iterable<Statement> iter = getIterable(resource, LAST_MODIFIED_DATE);
        return extractSingleCalendarValue(iter, LAST_MODIFIED_DATE);
    }

    /**
     * Gets the modified by user (if any) that was included within the statements.
     * @param resource the resource we are looking for properties of
     * @return the string that should be set for the MODIFIED_BY or null if it should be
     *         untouched
     */
    public static String getModifiedBy(final Resource resource) {
        final Iterable<Statement> iter = getIterable(resource, LAST_MODIFIED_BY);
        return extractSingleStringValue(iter, LAST_MODIFIED_BY);
    }

    private static String extractSingleStringValue(final Iterable<Statement> statements,
                                                   final Property predicate) {
        String textValue = null;
        for (final Statement added : statements) {
            if (textValue == null) {
                textValue = added.getObject().asLiteral().getString();
            } else {
                throw new MalformedRdfException(predicate + " may only appear once!");
            }
        }
        return textValue;
    }

    private static Calendar extractSingleCalendarValue(final Iterable<Statement> statements, final Property predicate) {
        Calendar cal = null;
        for (final Statement added : statements) {
            if (cal == null) {
                try {
                    cal = RelaxedPropertiesHelper.parseExpectedXsdDateTimeValue(added.getObject());
                } catch (final IllegalArgumentException e) {
                    throw new MalformedRdfException("Expected a xsd:datetime for " + predicate, e);
                }
            } else {
                throw new MalformedRdfException(predicate + " may only appear once!");
            }
        }
        return cal;
    }

    /**
     * Parses an RDFNode that is expected to be a literal of type xsd:dateTime into a Java Calendar
     * object.
     * @param node a node representing an xsd:dateTime literal
     * @return a Calendar representation of the expressed dateTime
     */
    private static Calendar parseExpectedXsdDateTimeValue(final RDFNode node) {
        final Object value = node.asLiteral().getValue();
        if (value instanceof XSDDateTime) {
            return ((XSDDateTime) value).asCalendar();
        } else {
            throw new IllegalArgumentException("Expected an xsd:dateTime!");
        }
    }

    private static Iterable<Statement> getIterable(final Resource resource, final Property predicate) {
        final var iterator = resource.listProperties(predicate);
        return () -> iterator;
    }

    /**
     * Several tests for invalid or disallowed RDF statements.
     * @param triple the triple to check.
     */
    public static void checkTripleForDisallowed(final Triple triple) {
        if (triple.getPredicate().equals(type().asNode()) && !triple.getObject().isVariable() &&
                !triple.getObject().isURI()) {
            // The object of a rdf:type triple is not a variable and not a URI.
            throw new MalformedRdfException(
                    String.format("Invalid rdf:type: %s", triple.getObject()));
        } else if (restrictedType.test(triple)) {
            // The object of a rdf:type triple has a restricted namespace.
            throw new ServerManagedTypeException(
                    String.format("The server managed type (%s) cannot be modified by the client.",
                            triple.getObject()));
        } else if (isManagedPredicate.test(createProperty(triple.getPredicate().getURI()))) {
            // The predicate is server managed.
            final var message = String.format("The server managed predicate (%s) cannot be modified by the client.",
                    triple.getPredicate());
            if (isRelaxablePredicate.test(createProperty(triple.getPredicate().getURI()))) {
                // It is a relaxable predicate so throw the appropriate exception.
                throw new RelaxableServerManagedPropertyException(message);
            }
            throw new ServerManagedPropertyException(message);
        }
    }

    // Prevent instantiation
    private RelaxedPropertiesHelper() {

    }
}
