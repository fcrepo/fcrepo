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
package org.fcrepo.kernel.modeshape.rdf.impl;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyValueIterator;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.stream.Stream;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_IS_MEMBER_OF_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBER_SUBJECT;
import static org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.modeshape.rdf.impl.ReferencesRdfContext.REFERENCE_TYPES;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;

/**
 * @author cabeer
 * @author ajs6f
 * @since 10/7/14
 */
public class LdpIsMemberOfRdfContext extends NodeRdfContext {
    private final ValueConverter valueConverter;

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws javax.jcr.RepositoryException if repository exception
     */
    public LdpIsMemberOfRdfContext(final FedoraResource resource,
                                   final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        valueConverter = new ValueConverter(getJcrNode(resource).getSession(), translator());
        final FedoraResource container = resource.getContainer();

        if (container != null
                && (container.hasType(LDP_DIRECT_CONTAINER) || container.hasType(LDP_INDIRECT_CONTAINER))
                && container.hasProperty(LDP_IS_MEMBER_OF_RELATION)) {
            concat(concatIsMemberOfRelation(container));
        }
    }

    private Stream<Triple> concatIsMemberOfRelation(final FedoraResource container) throws RepositoryException {
        final Property property = getJcrNode(container).getProperty(LDP_IS_MEMBER_OF_RELATION);

        final Resource memberRelation = createResource(property.getString());
        final Node membershipResource = getMemberResource(container);

        if (membershipResource == null) {
            return empty();
        }

        final String insertedContainerProperty;

        if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            if (container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
                insertedContainerProperty = getJcrNode(container).getProperty(LDP_INSERTED_CONTENT_RELATION)
                    .getString();
            } else {
                return empty();
            }
        } else {
            insertedContainerProperty = MEMBER_SUBJECT.getURI();
        }

        if (insertedContainerProperty.equals(MEMBER_SUBJECT.getURI())) {
            return of(create(subject(), memberRelation.asNode(), membershipResource));
        } else if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            final String insertedContentProperty = getPropertyNameFromPredicate(getJcrNode(resource()), createResource
                    (insertedContainerProperty), null);

            if (resource().hasProperty(insertedContentProperty)) {
                return iteratorToStream(new PropertyValueIterator(
                            getJcrNode(resource()).getProperty(insertedContentProperty)))
                        .map(valueConverter::convert)
                        .filter(n -> n.isURIResource())
                        .filter(n -> translator().inDomain(n.asResource()))
                        .map(s -> create(s.asNode(), memberRelation.asNode(), membershipResource));
            }
        }
        return empty();
    }

    /**
     * Get the membership resource relation asserted by the container
     * @param parent
     * @return
     * @throws RepositoryException
     */
    private Node getMemberResource(final FedoraResource parent) throws RepositoryException {
        final Node membershipResource;

        if (parent.hasProperty(LDP_MEMBER_RESOURCE)) {
            final Property memberResource = getJcrNode(parent).getProperty(LDP_MEMBER_RESOURCE);

            if (REFERENCE_TYPES.contains(memberResource.getType())) {
                membershipResource = nodeConverter().convert(memberResource.getNode()).asNode();
            } else {
                membershipResource = createURI(memberResource.getString());
            }
        } else {
            membershipResource = uriFor(parent);
        }

        return membershipResource;
    }
}
