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
package org.fcrepo.kernel.impl.rdf.impl.mappings;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.google.common.base.Converter;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.impl.rdf.converters.PropertyConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.slf4j.Logger;
import com.google.common.base.Function;
import com.hp.hpl.jena.graph.Triple;

/**
 * Utility for moving from JCR properties to RDF triples.
 *
 * @author ajs6f
 * @since Oct 10, 2013
 */
public class PropertyToTriple implements
        Function<Property, Iterator<Triple>> {

    private static final PropertyConverter propertyConverter = new PropertyConverter();
    private final ValueConverter valueConverter;
    private Converter<Node, Resource> graphSubjects;

    private static final Logger LOGGER = getLogger(PropertyToTriple.class);

    /**
     * Default constructor. We require a {@link Converter} in order to
     * construct the externally-meaningful RDF subjects of our triples.
     *
     * @param graphSubjects
     */
    public PropertyToTriple(final Session session, final Converter<Resource, FedoraResource> graphSubjects) {
        this.valueConverter = new ValueConverter(session, graphSubjects);
        this.graphSubjects = nodeToResource(graphSubjects);
    }

    /**
     * This nightmare of Java signature verbosity is a curried transformation.
     * We want to go from an iterator of JCR {@link Property} to an iterator
     * of RDF {@link Triple}s. An annoyance: some properties may produce several
     * triples (multi-valued properties). So we cannot find a simple Property to
     * Triple mapping. Instead, we wax clever and offer a function from any
     * specific property to a new function, one that takes multiple values (such
     * as occur in our multi-valued properties) to multiple triples. In other
     * words, this is a function the outputs of which are functions specific to
     * a given JCR property. Each output knows how to take any specific value of
     * its specific property to a triple representing the fact that its specific
     * property obtains that specific value on the node to which that property
     * belongs. All of this is useful because with these operations represented
     * as functions instead of ordinary methods, which may have side-effects, we
     * can use efficient machinery to manipulate iterators of the objects in
     * which we are interested, and that's exactly what we want to do in this
     * class. See {@link org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext#triplesFromProperties} for an
     * example of the use of this class with {@link ZippingIterator}.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Currying">Currying</a>
     */
    @Override
    public Iterator<Triple> apply(final Property p) {
        return Iterators.transform(new PropertyValueIterator(p), new Function<Value, Triple>() {

            @Override
            public Triple apply(final Value v) {
                return propertyvalue2triple(p, v);
            }
        });
    }

    /**
     * @param p A JCR {@link Property}
     * @param v The {@link Value} of that Property to use (in the case of
     *        multi-valued properties)  For single valued properties this
     *        must be that single value.
     * @return An RDF {@link Triple} representing that property.
     */
    private Triple propertyvalue2triple(final Property p, final Value v) {
        LOGGER.trace("Rendering triple for Property: {} with Value: {}", p, v);
        try {

            final Triple triple = create(graphSubjects.convert(p.getParent()).asNode(),
                    propertyConverter.convert(p).asNode(),
                    convertObject(p, v));

            LOGGER.trace("Created triple: {} ", triple);
            return triple;
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    private com.hp.hpl.jena.graph.Node convertObject(final Property p, final Value v) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node object = valueConverter.convert(v).asNode();

        if (object.isLiteral()) {
            final String propertyName = p.getName();
            final int i = propertyName.indexOf("@");

            if (i > 0) {
                final LiteralLabel literal = object.getLiteral();
                final String datatypeURI = literal.getDatatypeURI();

                if (datatypeURI.isEmpty() || datatypeURI.equals(XSDDatatype.XSDstring.getURI())) {

                    final String lang = propertyName.substring(i + 1);
                    return createLiteral(literal.getLexicalForm(), lang, literal.getDatatype());
                }
            }
        }

        return object;
    }

}
