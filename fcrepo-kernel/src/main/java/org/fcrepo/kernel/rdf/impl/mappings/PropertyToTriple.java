/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.rdf.impl.mappings;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterators.transform;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getPredicateForProperty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.slf4j.Logger;
import com.google.common.base.Function;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * Utility for moving from JCR properties to RDF triples.
 *
 * @author ajs6f
 * @date Oct 10, 2013
 */
public class PropertyToTriple implements
        Function<Property, Function<Iterator<Value>, Iterator<Triple>>> {

    private GraphSubjects graphSubjects;

    private final static Logger LOGGER = getLogger(PropertyToTriple.class);

    /**
     * Default constructor. We require a {@link GraphSubjects} in order to
     * construct the externally-meaningful RDF subjects of our triples.
     *
     * @param graphSubjects
     */
    public PropertyToTriple(final GraphSubjects graphSubjects) {
        this.graphSubjects = graphSubjects;
    }

    /**
     * This nightmare of Java signature verbosity is a curried transformation.
     * We want to go from an iterator of JCR {@link Properties} to an iterator
     * of RDF {@link Triple}s. An annoyance: some properties may produce several
     * triples (multi-valued properties). So we cannot find a simple Property ->
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
     * class. See {@link PropertiesRdfContext#triplesFromProperties} for an
     * example of the use of this class with {@link ZippingIterator}.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Currying">Currying</a>
     */
    @Override
    public Function<Iterator<Value>, Iterator<Triple>> apply(final Property p) {
        return new Function<Iterator<Value>, Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final Iterator<Value> vs) {
                return transform(vs, new Function<Value, Triple>() {

                    @Override
                    public Triple apply(final Value v) {
                        return propertyvalue2triple(p, v);
                    }
                });

            }
        };

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
            final Triple triple =
                create(getGraphSubject(p.getParent()), getPredicateForProperty
                        .apply(p).asNode(), propertyvalue2node(p, v));
            LOGGER.trace("Created triple: {} ", triple);
            return triple;
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    private Node propertyvalue2node(final Property p, final Value v) {
        try {
            switch (v.getType()) {
                case BOOLEAN:
                    return literal2node(v.getString());
                case DATE:
                    return literal2node(v.getDate());
                case DECIMAL:
                    return literal2node(v.getDecimal());
                case DOUBLE:
                    return literal2node(v.getDouble());
                case LONG:
                    return literal2node(v.getLong());
                case URI:
                    return createResource(v.getString()).asNode();
                case REFERENCE:
                case WEAKREFERENCE:
                case PATH:
                    return traverseLink(p, v);
                default:
                    return literal2node(v.getString());
            }
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    private static Node literal2node(final Object literal) {
        final Node result = createTypedLiteral(literal).asNode();
        LOGGER.trace("Converting {} into {}", literal, result);
        return result;
    }

    private Node traverseLink(final Property p, final Value v)
        throws RepositoryException {
        final javax.jcr.Node refNode;
        if (v.getType() == PATH) {
            refNode = p.getParent().getNode(v.getString());
        } else {
            refNode = p.getSession().getNodeByIdentifier(v.getString());
        }
        return getGraphSubject(refNode);
    }

    private Node getGraphSubject(final javax.jcr.Node n)
        throws RepositoryException {
        return graphSubjects.getGraphSubject(n).asNode();
    }

}
