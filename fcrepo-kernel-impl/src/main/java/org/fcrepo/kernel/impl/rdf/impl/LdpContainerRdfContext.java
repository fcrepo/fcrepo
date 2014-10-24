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
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.slf4j.Logger;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Iterator;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_CONTAINER;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_IS_MEMBER_OF_RELATION;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
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
        final PropertyIterator properties = new PropertyIterator(resource.getNode().getReferences(LDP_MEMBER_RESOURCE));

        if (properties.hasNext()) {
            LOGGER.trace("Found membership containers for {}", resource);
            concat(membershipContext(properties));
        }

        if (!resource.hasProperty(LDP_MEMBER_RESOURCE) && resource.hasType(LDP_CONTAINER)) {
            concat(memberRelations(resource));
        }
    }

    private Iterator<Triple> membershipContext(final PropertyIterator properties) {
        return Iterators.concat(Iterators.transform(properties, nodes2triples()));
    }

    private Function<Property, Iterator<Triple>> nodes2triples() {
        return new Function<Property,Iterator<Triple>>() {

            @Override
            public Iterator<Triple> apply(final Property input) {
                try {
                    final FedoraResource resource = nodeConverter.convert(input.getParent());

                    return memberRelations(resource);
                } catch (RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        };
    }

    /**
     * Get the member relations assert on the subject by the given node
     * @param resource
     * @return
     * @throws RepositoryException
     */
    private Iterator<Triple> memberRelations(final FedoraResource resource) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node memberRelation;

        if (resource.hasProperty(LDP_HAS_MEMBER_RELATION)) {
            final Property property = resource.getProperty(LDP_HAS_MEMBER_RELATION);
            memberRelation = NodeFactory.createURI(property.getString());
        } else if (resource.hasProperty(LDP_IS_MEMBER_OF_RELATION)) {
            return Collections.emptyIterator();
        } else {
            memberRelation = LDP_MEMBER.asNode();
        }

        final Iterator<FedoraResource> memberNodes = resource.getChildren();
        return Iterators.transform(memberNodes, new Function<FedoraResource, Triple>() {

            @Override
            public Triple apply(final FedoraResource child) {
                final com.hp.hpl.jena.graph.Node childSubject;
                if (child instanceof Datastream) {
                    childSubject = translator().reverse().convert(((Datastream) child).getBinary()).asNode();
                } else {
                    childSubject = translator().reverse().convert(child).asNode();
                }
                return create(subject(), memberRelation, childSubject);

            }
        });
    }

}
