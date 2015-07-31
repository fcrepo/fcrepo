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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.utils.UncheckedFunction;
import org.fcrepo.kernel.api.utils.UncheckedPredicate;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyValueIterator;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Iterator;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.Collections.emptyIterator;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.api.utils.UncheckedFunction.uncheck;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link org.fcrepo.kernel.api.utils.iterators.RdfStream} that contains {@link Triple}s linking from LDP
 * DirectContainers or IndirectContainers to their members.
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */
public class LdpContainerRdfContext extends NodeRdfContext {
    private static final Logger LOGGER = getLogger(ChildrenRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public LdpContainerRdfContext(final FedoraResource resource,
                                  final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);
        final Iterator<Property> memberReferences = resource.getNode().getReferences(LDP_MEMBER_RESOURCE);
        final Iterator<Property> properties = Iterators.filter(memberReferences, UncheckedPredicate.uncheck(p -> {
            final Node container = p.getParent();
            return container.isNodeType(LDP_DIRECT_CONTAINER) || container.isNodeType(LDP_INDIRECT_CONTAINER);
        }));

        if (properties.hasNext()) {
            LOGGER.trace("Found membership containers for {}", resource);
            concat(membershipContext(properties));
        }
    }

    private Iterator<Triple> membershipContext(final Iterator<Property> properties) {
        return flatMap(properties, uncheck(p -> memberRelations(nodeConverter.convert(p.getParent()))));
    }

    /**
     * Get the member relations asserted on the subject by the given node
     * @param container to check for members
     * @return
     * @throws RepositoryException
     */
    private Iterator<Triple> memberRelations(final FedoraResource container) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node memberRelation;

        if (container.hasProperty(LDP_HAS_MEMBER_RELATION)) {
            // if there is an ldp:memberRelation property, use its value as the predicate
            final Property property = container.getProperty(LDP_HAS_MEMBER_RELATION);
            memberRelation = createURI(property.getString());
        } else if (container.hasType(LDP_BASIC_CONTAINER)) {
            // otherwise, use ldp:hasMember for BasicContainers
            memberRelation = LDP_MEMBER.asNode();
        } else {
            // if we can't find a suitable predicate, then there's nothing for us to do
            return emptyIterator();
        }

        final String insertedContainerProperty;

        if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            if (container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
                // IndirectContainers should declare the insertedContentRelation to identify the container
                insertedContainerProperty = container.getProperty(LDP_INSERTED_CONTENT_RELATION).getString();
            } else {
                return emptyIterator();
            }
        } else {
            // other containers used a fixed property
            insertedContainerProperty = MEMBER_SUBJECT.getURI();
        }

        return flatMap(container.getChildren(),
                UncheckedFunction.<FedoraResource, Iterator<Triple>>uncheck(child -> {
                    final com.hp.hpl.jena.graph.Node childSubject = child instanceof NonRdfSourceDescription
                            ? uriFor(((NonRdfSourceDescription) child).getDescribedResource()) : uriFor(child);

                    if (insertedContainerProperty.equals(MEMBER_SUBJECT.getURI())) {
                        // in the simple case, we can create the link
                        return singletonIterator(create(subject(), memberRelation, childSubject));
                    }

                    // for IndirectContainers, we need to find the property or an inbound reference to it
                    String insertedContentProperty = getPropertyNameFromPredicate(resource().getNode(),
                            createResource(insertedContainerProperty), null);

                    if (child.hasProperty(insertedContentProperty)) {
                        // do nothing, insertedContentProperty is good

                    } else if (child.hasProperty(getReferencePropertyName(insertedContentProperty))) {
                        // The insertedContentProperty is a pseudo reference property
                        insertedContentProperty = getReferencePropertyName(insertedContentProperty);

                    } else {
                        // No property found!
                        return emptyIterator();
                    }

                    final PropertyValueIterator values =
                            new PropertyValueIterator(child.getProperty(insertedContentProperty));

                    return Iterators.transform(values, v -> create(subject(), memberRelation,
                            new ValueConverter(session(), translator()).convert(v).asNode()));

                }));
    }
}
