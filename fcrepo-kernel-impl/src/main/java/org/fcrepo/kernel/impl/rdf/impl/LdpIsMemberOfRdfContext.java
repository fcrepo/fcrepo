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
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyValueIterator;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.Iterator;

import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_IS_MEMBER_OF_RELATION;
import static org.fcrepo.kernel.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;

/**
 * @author cabeer
 * @since 10/7/14
 */
public class LdpIsMemberOfRdfContext extends NodeRdfContext {
    private final ValueConverter valueConverter;

    /**
     * Default constructor.
     *
     * @param resource
     * @param idTranslator
     * @throws javax.jcr.RepositoryException
     */
    public LdpIsMemberOfRdfContext(final FedoraResource resource,
                                   final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        valueConverter = new ValueConverter(session(), translator());
        final FedoraResource container = resource.getContainer();

        if (container != null
                && (container.hasType(LDP_DIRECT_CONTAINER) || container.hasType(LDP_INDIRECT_CONTAINER))
                && container.hasProperty(LDP_IS_MEMBER_OF_RELATION)) {
            concatIsMemberOfRelation(container);
        }
    }

    private void concatIsMemberOfRelation(final FedoraResource container) throws RepositoryException {
        final Property property = container.getProperty(LDP_IS_MEMBER_OF_RELATION);

        final Resource memberRelation = createResource(property.getString());
        final Resource membershipResource = getMemberResource(container);

        if (membershipResource == null) {
            return;
        }

        final String insertedContainerProperty;

        if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            if (container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
                insertedContainerProperty = container.getProperty(LDP_INSERTED_CONTENT_RELATION).getString();
            } else {
                return;
            }
        } else {
            insertedContainerProperty = RdfLexicon.MEMBER_SUBJECT.getURI();
        }

        if (insertedContainerProperty.equals(RdfLexicon.MEMBER_SUBJECT.getURI())) {
            concat(create(subject(), memberRelation.asNode(), membershipResource.asNode()));
        } else if (container.hasType(LDP_INDIRECT_CONTAINER)) {
            final String insertedContentProperty = getPropertyNameFromPredicate(resource().getNode(), createResource
                    (insertedContainerProperty), null);

            if (!resource().hasProperty(insertedContentProperty)) {
                return;
            }

            final PropertyValueIterator values
                    = new PropertyValueIterator(resource().getProperty(insertedContentProperty));

            final Iterator<RDFNode> insertedContentRelations = Iterators.filter(
                    Iterators.transform(values, valueConverter),
                    new Predicate<RDFNode>() {
                        @Override
                        public boolean apply(final RDFNode input) {
                            return input.isURIResource() && translator().inDomain(input.asResource());
                        }
                    });

            concat(Iterators.transform(insertedContentRelations, new Function<RDFNode, Triple>() {
                @Override
                public Triple apply(final RDFNode input) {
                    return create(input.asNode(), memberRelation.asNode(), membershipResource.asNode());
                }
            }));

        }
    }

    /**
     * Get the membership resource relation asserted by the container
     * @param parent
     * @return
     * @throws RepositoryException
     */
    private Resource getMemberResource(final FedoraResource parent) throws RepositoryException {
        final Resource membershipResource;

        if (parent.hasProperty(LDP_MEMBER_RESOURCE)) {
            final Property memberResource = parent.getProperty(LDP_MEMBER_RESOURCE);

            final int type = memberResource.getType();
            if ( type == REFERENCE || type == WEAKREFERENCE || type == PATH) {
                membershipResource = nodeConverter().convert(memberResource.getNode());
            } else {
                membershipResource = createResource(memberResource.getString());
            }
        } else {
            membershipResource = translator().reverse().convert(parent);
        }

        return membershipResource;
    }
}
