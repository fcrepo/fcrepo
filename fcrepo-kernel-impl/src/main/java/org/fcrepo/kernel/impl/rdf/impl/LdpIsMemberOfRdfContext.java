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

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import static com.hp.hpl.jena.graph.Triple.create;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_IS_MEMBER_OF_RELATION;
import static org.fcrepo.jcr.FedoraJcrTypes.LDP_MEMBER_RESOURCE;

/**
 * @author cabeer
 * @since 10/7/14
 */
public class LdpIsMemberOfRdfContext extends NodeRdfContext {
    /**
     * Default constructor.
     *
     * @param resource
     * @param graphSubjects
     * @throws javax.jcr.RepositoryException
     */
    public LdpIsMemberOfRdfContext(final FedoraResource resource,
                                   final IdentifierConverter<Resource, FedoraResource> graphSubjects)
            throws RepositoryException {
        super(resource, graphSubjects);

        final FedoraResource container = resource.getContainer();

        if (container != null) {

            concatIsMemberOfRelation(container);
        }
    }

    private void concatIsMemberOfRelation(final FedoraResource container) throws RepositoryException {
        if (container.hasProperty(LDP_IS_MEMBER_OF_RELATION)) {
            final Property property = container.getProperty(LDP_IS_MEMBER_OF_RELATION);

            final Resource memberRelation = ResourceFactory.createResource(property.getString());

            final Resource membershipResource = getMemberResource(container);

            if (membershipResource != null) {
                concat(create(subject(), memberRelation.asNode(), membershipResource.asNode()));
            }
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
                membershipResource = ResourceFactory.createResource(memberResource.getString());
            }
        } else {
            membershipResource = graphSubjects().reverse().convert(parent);
        }

        return membershipResource;
    }
}
