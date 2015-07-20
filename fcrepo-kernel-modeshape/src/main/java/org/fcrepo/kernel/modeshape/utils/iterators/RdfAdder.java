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
package org.fcrepo.kernel.modeshape.utils.iterators;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Consumes an {@link org.fcrepo.kernel.api.utils.iterators.RdfStream} by adding its contents to the
 * JCR.
 *
 * @see RdfRemover
 * @author ajs6f
 * @since Oct 24, 2013
 */
public class RdfAdder extends PersistingRdfStreamConsumer {

    private static final Logger LOGGER = getLogger(RdfAdder.class);

    /**
     * Ordinary constructor.
     *
     * @param idTranslator the id translator
     * @param session the session
     * @param stream the rdf stream
     */
    public RdfAdder(final IdentifierConverter<Resource, FedoraResource> idTranslator, final Session session,
        final RdfStream stream) {
        super(idTranslator, session, stream);
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource, final FedoraResource resource)
            throws RepositoryException {
        jcrRdfTools().addMixin(resource, mixinResource, stream().namespaces());
    }


    @Override
    protected void operateOnProperty(final Statement t, final FedoraResource resource) throws RepositoryException {
        LOGGER.debug("Adding property from triple: {} to resource: {}.", t, resource
                .getPath());

        jcrRdfTools().addProperty(resource, t.getPredicate(), t.getObject(), stream().namespaces());
    }
}
