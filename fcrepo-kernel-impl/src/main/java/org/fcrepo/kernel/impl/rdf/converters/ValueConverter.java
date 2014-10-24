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
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import java.util.regex.Pattern;

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
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/8/14
 */
public class ValueConverter extends Converter<Value, RDFNode> {

    private static final Logger LOGGER = getLogger(ValueConverter.class);
    public static final String LITERAL_TYPE_SEP = "\30^^\30";

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
                case STRING:
                    return stringliteral2node(value.getString());
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
                return valueFactory.createValue(resource.toString(), URI);
            }

            if (resource.isResource()) {
                // a non-URI resource (e.g. a blank node)
                return valueFactory.createValue(resource.toString(), UNDEFINED);
            }

            final Literal literal = resource.asLiteral();
            final RDFDatatype dataType = literal.getDatatype();

            final String dataTypeUri;

            if (dataType == null) {
                dataTypeUri = "";
            } else {
                dataTypeUri = dataType.getURI();
            }

            return valueFactory.createValue(literal.getString()
                    + LITERAL_TYPE_SEP + dataTypeUri
                    + LITERAL_TYPE_SEP + literal.getLanguage(), STRING);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }


    private static Literal literal2node(final Object o) {
        return ResourceFactory.createTypedLiteral(o);
    }

    private static Literal stringliteral2node(final String literal) {
        final String[] split = literal.split(Pattern.quote(LITERAL_TYPE_SEP), 3);

        if (split.length == 1) {
            return literal2node(split[0]);
        } else if (split.length == 3 && !split[2].isEmpty()) {
            return createLangLiteral(split[0], split[2]);
        } else if (split[1].isEmpty()) {
            return createPlainLiteral(split[0]);
        } else {
            return createTypedLiteral(split[0], NodeFactory.getType(split[1]));
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
