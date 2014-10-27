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
import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import java.math.BigDecimal;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
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
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/8/14
 */
public class ValueConverter extends Converter<Value, RDFNode> {

    private static final Logger LOGGER = getLogger(ValueConverter.class);
    private static final String LITERAL_TYPE_SEP = "\30^^\30";
    private static final String URI_SUFFIX = "URI";

    private final Session session;
    private final Converter<Node, Resource> graphSubjects;

    /**
     * Convert values between JCR values and RDF objects with the given session and subjects
     * @param session
     * @param graphSubjects
     */
    public ValueConverter(final Session session,
                          final Converter<Resource, FedoraResource> graphSubjects) {
        this.session = session;
        this.graphSubjects = nodeToResource(graphSubjects);
    }

    @Override
    protected RDFNode doForward(final Value value) {
        try {
            switch (value.getType()) {
                case BOOLEAN:
                    return literal2node(value.getBoolean());
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
                    return stringliteral2node(value.getString());
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
                return valueFactory.createValue(resource.toString() + LITERAL_TYPE_SEP + URI_SUFFIX, STRING);
            }

            if (resource.isResource()) {
                // a non-URI resource (e.g. a blank node)
                return valueFactory.createValue(resource.toString(), UNDEFINED);
            }

            final Literal literal = resource.asLiteral();
            final RDFDatatype dataType = literal.getDatatype();
            final Object rdfValue = literal.getValue();

            if (dataType == null && rdfValue instanceof String
                    || (dataType != null && dataType.equals(XSDDatatype.XSDstring))) {
                // short-circuit the common case
                return valueFactory.createValue(literal.getString(), STRING);
            } else if (rdfValue instanceof Boolean) {
                return valueFactory.createValue((Boolean) rdfValue);
            } else if (rdfValue instanceof Byte
                    || (dataType != null && dataType.getJavaClass() == Byte.class)) {

                return valueFactory.createValue(literal.getByte());
            } else if (rdfValue instanceof Double) {
                return valueFactory.createValue(literal.getDouble());
            } else if (rdfValue instanceof BigDecimal) {
                return valueFactory.createValue((BigDecimal)literal.getValue());
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
            } else if (dataType != null && !dataType.getURI().isEmpty()) {
                return valueFactory.createValue(literal.getString() + LITERAL_TYPE_SEP + dataType.getURI(), STRING);
            } else {
                return valueFactory.createValue(literal.getString(), STRING);
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private static Literal literal2node(final Object literal) {
        final Literal result = createTypedLiteral(literal);
        LOGGER.trace("Converting {} into {}", literal, result);
        return result;
    }


    private static RDFNode stringliteral2node(final String literal) {
        final int i = literal.indexOf(LITERAL_TYPE_SEP);

        if (i < 0) {
            return literal2node(literal);
        } else {
            final String value = literal.substring(0, i);
            final String datatypeURI = literal.substring(i + LITERAL_TYPE_SEP.length());

            if (datatypeURI.equals("URI")) {
                return createResource(value);
            } else {
                return createTypedLiteral(value, new BaseDatatype(datatypeURI));
            }
        }
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

    private RDFNode getGraphSubject(final javax.jcr.Node n) {
        return graphSubjects.convert(n);
    }
}
