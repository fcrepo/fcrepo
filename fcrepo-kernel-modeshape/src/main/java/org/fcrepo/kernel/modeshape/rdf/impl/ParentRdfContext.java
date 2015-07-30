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

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;

import static com.hp.hpl.jena.graph.Triple.create;
import static org.fcrepo.kernel.api.FedoraJcrTypes.VERSIONABLE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PARENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/16/14
 */
public class ParentRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(ParentRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public ParentRdfContext(final FedoraResource resource,
                            final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        if (resource.getNode().getDepth() > 0) {
            LOGGER.trace("Determined that this resource has a parent.");
            // The parent node of a frozen node for a versionable resource is not a node we want to link to because it
            // is in the jcr:system space
            if (!resource().isFrozenResource() || !resource().getUnfrozenResource().hasType(VERSIONABLE)) {
                concat(create(subject(), HAS_PARENT.asNode(), uriFor(resource().getContainer())));
            }
        }
    }
}
