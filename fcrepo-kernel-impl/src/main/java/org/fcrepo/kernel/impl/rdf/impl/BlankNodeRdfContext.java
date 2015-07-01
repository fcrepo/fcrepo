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
package org.fcrepo.kernel.impl.rdf.impl;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueIterator;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
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

    private static final List<Integer> referencePropertyTypes = asList(PATH,REFERENCE,WEAKREFERENCE);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public BlankNodeRdfContext(final FedoraResource resource,
                               final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        concat(Iterators.concat(Iterators.transform(getBlankNodesIterator(), new Function<Node, RdfStream>() {
            @Override
            public RdfStream apply(final Node node) {
                final FedoraResource resource = nodeConverter.convert(node);

                return resource.getTriples(idTranslator, ImmutableList.of(TypeRdfContext.class,
                        PropertiesRdfContext.class,
                        BlankNodeRdfContext.class));
            }
        })));

    }

    private Iterator<Node> getBlankNodesIterator() throws RepositoryException {
        final Iterator<Property> properties = resource().getNode().getProperties();

        final Iterator<Property> references = Iterators.filter(properties, filterReferenceProperties);

        final Iterator<Node> nodes = Iterators.transform(new PropertyValueIterator(references), getNodesForValue);

        return Iterators.filter(nodes, isBlankNode::test);
    }


    private static final Predicate<Property> filterReferenceProperties = new Predicate<Property>() {

        @Override
        public boolean apply(final Property property) {
            try {
                final int type = property.getType();
                return referencePropertyTypes.contains(type);
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
    };

    private final Function<Value, Node> getNodesForValue = new Function<Value, Node>() {
        @Override
        public Node apply(final Value v) {
            try {
                final Node refNode;
                if (v.getType() == PATH) {
                    refNode = resource().getNode().getSession().getNode(v.getString());
                } else {
                    refNode = resource().getNode().getSession().getNodeByIdentifier(v.getString());
                }
                return refNode;
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
    };

}
