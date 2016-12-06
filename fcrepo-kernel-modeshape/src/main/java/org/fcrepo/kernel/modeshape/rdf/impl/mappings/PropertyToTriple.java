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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.Triple.create;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeToResource;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.function.Function;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.google.common.base.Converter;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Triple;
import org.slf4j.Logger;

/**
 * Utility for moving from JCR properties to RDF triples.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class PropertyToTriple implements Function<Property, Stream<Triple>> {

    private static final Logger LOGGER = getLogger(PropertyToTriple.class);

    private static final PropertyConverter propertyConverter = new PropertyConverter();
    private final ValueConverter valueConverter;
    private final Converter<Node, Resource> translator;

    /**
     * Default constructor. We require a {@link Converter} in order to construct the RDF subjects of our triples.
     *
     * @param converter a converter between RDF and the Fedora model
     * @param session the JCR session
     */
    public PropertyToTriple(final Session session, final Converter<Resource, FedoraResource> converter) {
        this.valueConverter = new ValueConverter(session, converter);
        this.translator = nodeToResource(converter);
    }

    @Override
    public Stream<Triple> apply(final Property p) {
        try {
            final org.apache.jena.graph.Node subject = translator.convert(p.getParent()).asNode();
            final org.apache.jena.graph.Node propPredicate = propertyConverter.convert(p).asNode();
            final String propertyName = p.getName();

            return iteratorToStream(new PropertyValueIterator(p)).filter(this::valueCanBeConverted).map(v -> {
                final org.apache.jena.graph.Node object = valueConverter.convert(v).asNode();
                if (object.isLiteral()) {
                    // unpack the name of the property for information about what kind of literal
                    final int i = propertyName.indexOf('@');
                    if (i > 0) {
                        final LiteralLabel literal = object.getLiteral();
                        final RDFDatatype datatype = literal.getDatatype();
                        final String datatypeURI = datatype.getURI();
                        if (datatypeURI.isEmpty() || datatype.equals(XSDstring)) {
                            // this is an RDF string literal and could involve an RDF lang tag
                            final String lang = propertyName.substring(i + 1);
                            final String lex = literal.getLexicalForm();
                            return create(subject, propPredicate, createLiteral(lex, lang, datatype));
                        }
                    }
                }
                return create(subject, propPredicate, object);
            });
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * This method tests if a given value can be converted.
     * The scenario when this may not be true is for (weak)reference properties that target an non-existent resource.
     * This scenario generally should not be possible, but the following bug introduced the possibility:
     *   https://jira.duraspace.org/browse/FCREPO-2323
     *
     * @param value to be tested for whether it can be converted to an RDFNode or not
     * @return true if value can be converted
     */
    private boolean valueCanBeConverted(final Value value) {
        try {
            valueConverter.convert(value);
            return true;
        } catch (final RepositoryRuntimeException e) {
            LOGGER.warn("Reference to non-existent resource encounterd: {}", value);
            return false;
        }
    };
}
