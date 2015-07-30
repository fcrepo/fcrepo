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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeToResource;
import java.util.Iterator;
import java.util.function.Function;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import com.google.common.base.Converter;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Triple;

/**
 * Utility for moving from JCR properties to RDF triples.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class PropertyToTriple implements Function<Property, Iterator<Triple>> {

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
    public Iterator<Triple> apply(final Property p) {
        try {
            final com.hp.hpl.jena.graph.Node subject = translator.convert(p.getParent()).asNode();
            final com.hp.hpl.jena.graph.Node propPredicate = propertyConverter.convert(p).asNode();
            final String propertyName = p.getName();

            return Iterators.transform(new PropertyValueIterator(p), v -> {
                final com.hp.hpl.jena.graph.Node object = valueConverter.convert(v).asNode();
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
}
