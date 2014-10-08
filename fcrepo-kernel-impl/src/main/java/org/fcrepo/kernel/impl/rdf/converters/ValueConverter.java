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
package org.fcrepo.kernel.impl.rdf.converters;

import com.google.common.base.Converter;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/8/14
 */
public class ValueConverter extends Converter<Value, RDFNode> {

    private static final Logger LOGGER = getLogger(ValueConverter.class);

    private final Session session;
    private final IdentifierConverter<Resource, javax.jcr.Node> graphSubjects;

    /**
     * Convert values between JCR values and RDF objects with the given session and subjects
     * @param session
     * @param graphSubjects
     */
    public ValueConverter(final Session session, final IdentifierConverter<Resource,javax.jcr.Node> graphSubjects) {
        this.session = session;
        this.graphSubjects = graphSubjects;
    }

    @Override
    protected RDFNode doForward(final Value value) {
        try {
            switch (value.getType()) {
                case BOOLEAN:
                    return literal2node(value.getString());
                case DATE:
                    return literal2node(value.getDate());
                case DECIMAL:
                    return literal2node(value.getDecimal());
                case DOUBLE:
                    return literal2node(value.getDouble());
                case LONG:
                    return literal2node(value.getLong());
                case URI:
                    return createResource(value.getString());
                case REFERENCE:
                case WEAKREFERENCE:
                case PATH:
                    return traverseLink(value);
                default:
                    return literal2node(value.getString());
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    protected Value doBackward(final RDFNode resource) {

        try {

            final ValueFactory valueFactory = session.getValueFactory();

            if (resource.isURIResource()) {
                // some random opaque URI
                return valueFactory.createValue(resource.toString(), URI);
            }

            if (resource.isResource()) {
                // a non-URI resource (e.g. a blank node)
                return valueFactory.createValue(resource.toString(), UNDEFINED);
            }

            final Literal literal = resource.asLiteral();
            final RDFDatatype dataType = literal.getDatatype();
            final Object rdfValue = literal.getValue();

            if (rdfValue instanceof Boolean) {
                return valueFactory.createValue((Boolean) rdfValue);
            } else if (rdfValue instanceof Byte
                    || (dataType != null && dataType.getJavaClass() == Byte.class)) {

                return valueFactory.createValue(literal.getByte());
            } else if (rdfValue instanceof Double) {
                return valueFactory.createValue(literal.getDouble());
            } else if (rdfValue instanceof Float) {
                return valueFactory.createValue(literal.getFloat());
            } else if (rdfValue instanceof Long
                    || (dataType != null && dataType.getJavaClass() == Long.class)) {
                return valueFactory.createValue(literal.getLong());
            } else if (rdfValue instanceof Short
                    || (dataType != null && dataType.getJavaClass() == Short.class)) {
                return valueFactory.createValue(literal.getShort());
            } else if (rdfValue instanceof Integer) {
                return valueFactory.createValue(literal.getInt());
            } else if (rdfValue instanceof XSDDateTime) {
                return valueFactory.createValue(((XSDDateTime) rdfValue)
                        .asCalendar());
            } else {
                return valueFactory.createValue(literal.getString(), STRING);
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private Literal literal2node(final Object literal) {
        final Literal result = ResourceFactory.createTypedLiteral(literal);
        LOGGER.trace("Converting {} into {}", literal, result);
        return result;
    }

    private RDFNode traverseLink(final Value v)
            throws RepositoryException {
        final javax.jcr.Node refNode;
        if (v.getType() == PATH) {
            refNode = session.getNode(v.getString());
        } else {
            refNode = session.getNodeByIdentifier(v.getString());
        }
        return getGraphSubject(refNode);
    }

    private RDFNode getGraphSubject(final javax.jcr.Node n)
            throws RepositoryException {
        return graphSubjects.reverse().convert(n);
    }
}
