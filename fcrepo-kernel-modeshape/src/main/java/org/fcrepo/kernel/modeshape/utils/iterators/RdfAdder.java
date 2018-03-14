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

import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;

import java.util.Iterator;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.modeshape.utils.NamespaceTools;
import org.slf4j.Logger;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * Consumes an {@link RdfStream} by adding its contents to the
 * JCR.
 *
 * @see RdfRemover
 * @author ajs6f
 * @since Oct 24, 2013
 */
public class RdfAdder extends PersistingRdfStreamConsumer {

    private static final Logger LOGGER = getLogger(RdfAdder.class);
    private Map<String, String> userNamespaces;

    /**
     * Ordinary constructor.
     *
     * @param idTranslator the id translator
     * @param session the session
     * @param stream the rdf stream
     * @param userNamespaces user-provided namespace mapping
     */
    public RdfAdder(final IdentifierConverter<Resource, FedoraResource> idTranslator, final Session session,
        final RdfStream stream, final Map<String, String> userNamespaces) {
        super(idTranslator, session, stream);
        this.userNamespaces = userNamespaces;
    }

    protected Map<String, String> getNamespaces(final Session session) {
        final Map<String, String> namespaces = NamespaceTools.getNamespaces(session);
        if (userNamespaces != null) {
            for (final Iterator<String> it = userNamespaces.keySet().iterator(); it.hasNext(); ) {
                final String prefix = it.next();
                final String uri = userNamespaces.get(prefix);
                if (!namespaces.containsKey(prefix) && !namespaces.containsValue(uri)) {
                    LOGGER.debug("Adding user-supplied namespace mapping {}: {}", prefix, uri);
                    namespaces.put(prefix, uri);
                } else {
                    LOGGER.debug("Not adding conflicting user-supplied namespace mapping {}: {}", prefix, uri);
                }
            }
        }
        return namespaces;
    }

    @Override
    protected void operateOnMixin(final Resource mixinResource, final FedoraResource resource)
            throws RepositoryException {
        jcrRdfTools().addMixin(resource, mixinResource, getNamespaces(getJcrNode(resource).getSession()));
    }


    @Override
    protected void operateOnProperty(final Statement t, final FedoraResource resource) throws RepositoryException {
        LOGGER.debug("Adding property from triple: {} to resource: {}.", t, resource
                .getPath());

        jcrRdfTools().addProperty(resource, t.getPredicate(), t.getObject(),
                getNamespaces(getJcrNode(resource).getSession()));
    }
}
