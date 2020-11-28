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

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.kernel.api.exception.MalformedRdfException;

import java.util.Calendar;

import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;

/**
 * Some server managed triples can have the prohibition on user-management overridden.  While
 * the server still updates them implicitly, it may be possible in some cases for a user
 * request to override them.
 *
 * @author Mike Durbin
 */
public class RelaxedPropertiesHelper {

    /**
     * Gets the created date (if any) that was included in the statements.
     * @param statements statements to consider
     * @return the date that should be set for the CREATED_DATE or null if it should be
     *         untouched
     */
    public static Calendar getCreatedDate(final Iterable<Statement> statements) {
        return extractSingleCalendarValue(statements, CREATED_DATE);
    }

    /**
     * Gets the created by user (if any) that is included within the statements.
     * @param statements statements to consider
     * @return the date that should be set for the CREATED_BY or null if it should be
     *         untouched
     */
    public static String getCreatedBy(final Iterable<Statement> statements) {
        return extractSingleStringValue(statements, CREATED_BY);
    }

    /**
     * Gets the modified date (if any) that was included within the statements.
     * @param statements statements to consider
     * @return the date that should be set for the LAST_MODIFIED_DATE or null if it should be
     *         untouched
     */
    public static Calendar getModifiedDate(final Iterable<Statement> statements) {
        return extractSingleCalendarValue(statements, LAST_MODIFIED_DATE);
    }

    /**
     * Gets the modified by user (if any) that was included within the statements.
     * @param statements statements to consider
     * @return the date that should be set for the MODIFIED_BY or null if it should be
     *         untouched
     */
    public static String getModifiedBy(final Iterable<Statement> statements) {
       return extractSingleStringValue(statements, LAST_MODIFIED_BY);
    }

    private static String extractSingleStringValue(final Iterable<Statement> statements, final Property predicate) {
        String username = null;
        for (final Statement added : statements) {
            if (added.getPredicate().equals(predicate)) {
                if (username == null) {
                    username = added.getObject().asLiteral().getString();
                } else {
                    throw new MalformedRdfException(predicate + " may only appear once!");
                }
            }
        }
        return username;
    }

    private static Calendar extractSingleCalendarValue(final Iterable<Statement> statements,
                                                       final Property predicate) {
        Calendar cal = null;
        for (final Statement added : statements) {
            if (added.getPredicate().equals(predicate)) {
                if (cal == null) {
                    cal = RelaxedPropertiesHelper.parseExpectedXsdDateTimeValue(added.getObject());
                } else {
                    throw new MalformedRdfException(predicate + " may only appear once!");
                }
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

    // Prevent instantiation
    private RelaxedPropertiesHelper() {

    }
}
