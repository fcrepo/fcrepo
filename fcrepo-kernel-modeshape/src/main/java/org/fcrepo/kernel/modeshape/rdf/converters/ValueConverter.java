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
package org.fcrepo.kernel.modeshape.rdf.converters;

import com.google.common.base.Converter;
import com.google.common.base.Splitter;
import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import java.util.Iterator;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createLangLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
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
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeToResource;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/8/14
 */
public class ValueConverter extends Converter<Value, RDFNode> {

    private static final Logger LOGGER = getLogger(ValueConverter.class);

    private final Session session;
    private final Converter<Node, Resource> graphSubjects;

    /**
     * Convert values between JCR values and RDF objects with the given session and subjects
     * @param session the session
     * @param graphSubjects the graph subjects
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

            if (resource.isAnon()) {
                // a non-URI resource (e.g. a blank node)
                return valueFactory.createValue(resource.toString(), UNDEFINED);
            }

            final RdfLiteralJcrValueBuilder rdfLiteralJcrValueBuilder = new RdfLiteralJcrValueBuilder();

            if (resource.isURIResource()) {
                rdfLiteralJcrValueBuilder.value(resource.asResource().getURI()).datatype("URI");
            } else {

                final Literal literal = resource.asLiteral();
                final RDFDatatype dataType = literal.getDatatype();

                rdfLiteralJcrValueBuilder.value(literal.getString()).datatype(dataType).lang(literal.getLanguage());
            }

            return valueFactory.createValue(rdfLiteralJcrValueBuilder.toString(), STRING);
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
        final RdfLiteralJcrValueBuilder rdfLiteralJcrValueBuilder = new RdfLiteralJcrValueBuilder(literal);

        if (rdfLiteralJcrValueBuilder.hasLang()) {
            return createLangLiteral(rdfLiteralJcrValueBuilder.value(), rdfLiteralJcrValueBuilder.lang());
        } else if (rdfLiteralJcrValueBuilder.isResource()) {
            return createResource(rdfLiteralJcrValueBuilder.value());
        } else if (rdfLiteralJcrValueBuilder.hasDatatypeUri()) {
            return createTypedLiteral(rdfLiteralJcrValueBuilder.value(), rdfLiteralJcrValueBuilder.datatype());
        } else {
            return createPlainLiteral(literal);
        }
    }

    private RDFNode traverseLink(final Value v) throws RepositoryException {
        return getGraphSubject(nodeForValue(session,v));
    }

    /**
     * Get the node that a property value refers to.
     * @param session Session to use to load the node.
     * @param v Value that refers to a node.
     * @return the JCR node
     * @throws RepositoryException When there is an error accessing the node.
     * @throws RepositoryRuntimeException When the value type is not PATH, REFERENCE or WEAKREFERENCE.
    **/
    public static javax.jcr.Node nodeForValue(final Session session, final Value v) throws RepositoryException {
        if (v.getType() == PATH) {
            return session.getNode(v.getString());
        } else if (v.getType() == REFERENCE || v.getType() == WEAKREFERENCE) {
            return session.getNodeByIdentifier(v.getString());
        } else {
            throw new RepositoryRuntimeException("Cannot convert value of type "
                    + PropertyType.nameFromValue(v.getType()) + " to a node reference");
        }
    }

    private RDFNode getGraphSubject(final javax.jcr.Node n) {
        return graphSubjects.convert(n);
    }

    protected static class RdfLiteralJcrValueBuilder {
        private static final String FIELD_DELIMITER = "\30^^\30";
        public static final Splitter JCR_VALUE_SPLITTER = Splitter.on(FIELD_DELIMITER);

        private String value;
        private String datatypeUri;
        private String lang;

        RdfLiteralJcrValueBuilder() {

        }

        public RdfLiteralJcrValueBuilder(final String literal) {
            this();

            final Iterator<String> tokenizer = JCR_VALUE_SPLITTER.split(literal).iterator();

            value = tokenizer.next();

            if (tokenizer.hasNext()) {
                datatypeUri = tokenizer.next();
            }

            if (tokenizer.hasNext()) {
                lang = tokenizer.next();
            }
        }

        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder();

            b.append(value);

            if (hasDatatypeUri()) {
                b.append(FIELD_DELIMITER);
                b.append(datatypeUri);
            } else if (hasLang()) {
                // if it has a language, but not a datatype, add a placeholder.
                b.append(FIELD_DELIMITER);
            }

            if (hasLang()) {
                b.append(FIELD_DELIMITER);
                b.append(lang);
            }

            return b.toString();

        }

        public String value() {
            return value;
        }

        public RDFDatatype datatype() {
            if (hasDatatypeUri()) {
                return new BaseDatatype(datatypeUri);
            }
            return null;
        }

        public String lang() {
            return lang;
        }

        public RdfLiteralJcrValueBuilder value(final String value) {
            this.value = value;
            return this;
        }

        public RdfLiteralJcrValueBuilder datatype(final String datatypeUri) {
            this.datatypeUri = datatypeUri;
            return this;
        }

        public RdfLiteralJcrValueBuilder datatype(final RDFDatatype datatypeUri) {
            if (datatypeUri != null && !datatypeUri.getURI().isEmpty()) {
                this.datatypeUri = datatypeUri.getURI();
            }
            return this;
        }

        public RdfLiteralJcrValueBuilder lang(final String lang) {
            this.lang = lang;
            return this;
        }

        public boolean hasLang() {
            return lang != null && !lang.isEmpty();
        }

        public boolean hasDatatypeUri() {
            return datatypeUri != null && !datatypeUri.isEmpty();
        }

        public boolean isResource() {
            return hasDatatypeUri() && datatypeUri.equals("URI");
        }
    }
}
