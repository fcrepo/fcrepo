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
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueIterator;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.Iterator;

import static com.google.common.collect.Iterators.singletonIterator;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Collections.emptyIterator;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */
public class LdpContainerRdfContext extends NodeRdfContext {
    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */
    public LdpContainerRdfContext(final FedoraResource resource,
                                  final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);
        final Iterator<Property> memberReferences = resource.getNode().getReferences(LDP_MEMBER_RESOURCE);
        final Iterator<Property> properties = Iterators.filter(memberReferences, isContainer );

        if (properties.hasNext()) {
            LOGGER.trace("Found membership containers for {}", resource);
            concat(membershipContext(properties));
        }
    }

    private static Predicate<Property> isContainer = new Predicate<Property>() {

        @Override
        public boolean apply(final Property property) {
            try {
                final Node container = property.getParent();
                return container.isNodeType(LDP_DIRECT_CONTAINER) || container.isNodeType(LDP_INDIRECT_CONTAINER);
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
    };

    private Iterator<Triple> membershipContext(final Iterator<Property> properties) {
        return Iterators.concat(Iterators.transform(properties, nodes2triples()));
    }

    private Function<Property, Iterator<Triple>> nodes2triples() {
        return new Function<Property,Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final Property property) {
                try {
                    final FedoraResource resource = nodeConverter.convert(property.getParent());
                    return memberRelations(resource);
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        };
    }

    /**
     * Get the member relations assert on the subject by the given node
     * @param container
     * @return
     * @throws RepositoryException
     */
    private Iterator<Triple> memberRelations(final FedoraResource container) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node memberRelation;

        if (container.hasProperty(LDP_HAS_MEMBER_RELATION)) {
            final Property property = container.getProperty(LDP_HAS_MEMBER_RELATION);
            memberRelation = createURI(property.getString());
        } else if (container.hasType(LDP_BASIC_CONTAINER)) {
            memberRelation = LDP_MEMBER.asNode();
        } else {
            return emptyIterator();
        }

        final String insertedContainerProperty;

        if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            if (container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
                insertedContainerProperty = container.getProperty(LDP_INSERTED_CONTENT_RELATION).getString();
            } else {
                return emptyIterator();
            }
        } else {
            insertedContainerProperty = MEMBER_SUBJECT.getURI();
        }

        final Iterator<FedoraResource> memberNodes = container.getChildren();

        return Iterators.concat(Iterators.transform(memberNodes, new Function<FedoraResource, Iterator<Triple>>() {
            @Override
            public Iterator<Triple> apply(final FedoraResource child) {

                try {
                    final com.hp.hpl.jena.graph.Node childSubject;
                    if (child instanceof NonRdfSourceDescription) {
                        childSubject = translator().reverse()
                                .convert(((NonRdfSourceDescription) child).getDescribedResource())
                                .asNode();
                    } else {
                        childSubject = translator().reverse().convert(child).asNode();
                    }

                    if (insertedContainerProperty.equals(MEMBER_SUBJECT.getURI())) {

                        return singletonIterator(create(subject(), memberRelation, childSubject));
                    }
                    final String insertedContentProperty = getPropertyNameFromPredicate(resource().getNode(),
                            createResource(insertedContainerProperty),
                            null);

                    if (!child.hasProperty(insertedContentProperty)) {
                        return emptyIterator();
                    }

                    final PropertyValueIterator values
                            = new PropertyValueIterator(child.getProperty(insertedContentProperty));

                    return Iterators.transform(values, new Function<Value, Triple>() {
                        @Override
                        public Triple apply(final Value input) {
                            final RDFNode membershipResource = new ValueConverter(session(), translator())
                                    .convert(input);
                            return create(subject(), memberRelation, membershipResource.asNode());
                        }
                    });
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        }));
    }
}
