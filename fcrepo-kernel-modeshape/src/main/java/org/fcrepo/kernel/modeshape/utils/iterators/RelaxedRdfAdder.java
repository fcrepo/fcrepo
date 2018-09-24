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

import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.slf4j.Logger;

/**
 * Consumes an {@link RdfStream} by adding its contents to the JCR with relaxed restrictions on server managed
 * properties.
 *
 * @author bbpennel
 */
public class RelaxedRdfAdder extends RdfAdder {

    private static final Logger LOGGER = getLogger(RelaxedRdfAdder.class);

    /**
     * Default constructor
     *
     * @param idTranslator translator
     * @param session session
     * @param stream rdf stream
     * @param userNamespaces namespaces
     */
    public RelaxedRdfAdder(final IdentifierConverter<Resource, FedoraResource> idTranslator, final Session session,
            final RdfStream stream, final Map<String, String> userNamespaces) {
        super(idTranslator, session, stream, userNamespaces);
    }

    @Override
    protected void operateOnTriple(final Statement input) throws MalformedRdfException {
        try {
            final Statement t = jcrRdfTools().skolemize(translator(), input, stream().topic().toString());

            final Resource subject = t.getSubject();
            final FedoraResource subjectNode = translator().convert(subject);

            // if this is a user-managed RDF type assertion, update the node's
            // mixins. If it isn't, treat it as a "data" property.
            if (t.getPredicate().equals(type) && t.getObject().isResource()) {
                final Resource mixinResource = t.getObject().asResource();

                LOGGER.debug("Operating on node: {} with mixin: {}.",
                        subjectNode.getPath(), mixinResource);
                operateOnMixin(mixinResource, subjectNode);
            } else {
                LOGGER.debug("Operating on node: {} from triple: {}.", subjectNode.getPath(), t);
                operateOnProperty(t, subjectNode);
            }
        } catch (final RepositoryException | RepositoryRuntimeException e) {
            throw new MalformedRdfException(e.getMessage(), e);
        }
    }

    @Override
    protected void operateOnProperty(final Statement t, final FedoraResource resource) throws RepositoryException {
        LOGGER.debug("Adding property from triple: {} to resource: {}.", t, resource
                .getPath());

        jcrRdfTools().addProperty(resource, t.getPredicate(), t.getObject(),
                getNamespaces(getJcrNode(resource).getSession()), true);
    }
}
