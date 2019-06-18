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
package org.fcrepo.kernel.modeshape.utils.iterators;

import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaces;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
import org.slf4j.Logger;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * Consumes an {@link RdfStream} by removing its contents from the
 * JCR.
 *
 * @see RdfAdder
 * @author ajs6f
 * @since Oct 24, 2013
 */
public class RdfRemover extends PersistingRdfStreamConsumer {

    private static final Logger LOGGER = getLogger(RdfRemover.class);

    /**
     * Ordinary constructor.
     *
     * @param idTranslator the id translator
     * @param session the session
     * @param stream the stream
     */
    public RdfRemover(final IdentifierConverter<Resource, FedoraResource> idTranslator, final Session session,
        final RdfStream stream) {
        super(idTranslator, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource,
        final FedoraResource resource) throws RepositoryException {

        final FedoraResource description = resource.getDescription();
        jcrRdfTools().removeMixin(description, mixinResource, getNamespaces(getJcrNode(description).getSession()));
    }

    @Override
    protected void operateOnProperty(final Statement t, final FedoraResource resource)
        throws RepositoryException {
        LOGGER.debug("Trying to remove property from triple: {} on resource: {}.", t, resource
                .getPath());
        final FedoraResource description = resource.getDescription();
        jcrRdfTools().removeProperty(description, t.getPredicate(), t.getObject(),
                getNamespaces(getJcrNode(description).getSession()));
    }

}
