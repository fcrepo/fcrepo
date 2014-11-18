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
package org.fcrepo.kernel.impl.rdf.impl;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueIterator;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.of;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isBlankNode;

/**
 * Embed all blank nodes in the RDF stream
 *
 * @author cabeer
 * @author ajs6f
 * @since 10/9/14
 */
public class BlankNodeRdfContext extends NodeRdfContext {

    private static final Set<Integer> REFERENCE_PROPERTY_TYPES = of(REFERENCE, WEAKREFERENCE, PATH);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */
    public BlankNodeRdfContext(final FedoraResource resource,
                               final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        concat(Iterators.concat(Iterators.transform(getBlankNodesIterator(), new Function<Node, RdfStream>() {

            @Override
            public RdfStream apply(final Node node) {
                final FedoraResource resource = nodeConverter.convert(node);

                return resource.getTriples(idTranslator, of(TypeRdfContext.class,
                        PropertiesRdfContext.class,
                        BlankNodeRdfContext.class));
            }
        })));
    }

    private Iterator<Node> getBlankNodesIterator() throws RepositoryException {
        final PropertyIterator properties = new PropertyIterator(resource().getNode().getProperties());

        final Iterator<Property> references = Iterators.filter(properties, filterReferenceProperties);

        final Iterator<Node> nodes = Iterators.transform(new PropertyValueIterator(references), getNodesForValue);

        return Iterators.filter(nodes, isBlankNode);
    }


    private static final Predicate<Property> filterReferenceProperties = new Predicate<Property>() {
        @Override
        public boolean apply(final Property p) {
            checkNotNull(p);
            try {
                return REFERENCE_PROPERTY_TYPES.contains(p.getType());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
    };

    private final Function<Value, Node> getNodesForValue = new Function<Value, Node>() {

        @Override
        public Node apply(final Value v) {
            checkNotNull(v);
            try {
                final Session session = resource().getNode().getSession();
                return v.getType() == PATH ? session.getNode(v.getString()) :
                        session.getNodeByIdentifier(v.getString());
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
    };

}