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
package org.fcrepo.kernel.impl.utils.iterators;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Consumes an {@link org.fcrepo.kernel.utils.iterators.RdfStream} by removing its contents from the
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

        jcrRdfTools().removeMixin(resource, mixinResource, stream().namespaces());
    }

    @Override
    protected void operateOnProperty(final Statement t, final FedoraResource resource)
        throws RepositoryException {
        LOGGER.debug("Trying to remove property from triple: {} on resource: {}.", t, resource
                .getPath());
        jcrRdfTools().removeProperty(resource, t.getPredicate(), t.getObject(), stream().namespaces());

    }
}
