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
package org.fcrepo.kernel.modeshape.observer;

import java.util.function.Function;

import org.fcrepo.kernel.api.observer.FedoraEvent;

import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.jcrProperties;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.utils.NamespaceTools.getNamespaceRegistry;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Andrew Woods
 * @author ajs6f
 * @since 11/22/14
 */
public class GetNamespacedProperties implements Function<FedoraEvent, FedoraEvent> {

    private static final Logger LOGGER = getLogger(SimpleObserver.class);

    private final Session session;

    /**
     * Constructor
     *
     * @param session used to get NamespaceRegistry
     */
    public GetNamespacedProperties(final Session session) {
        this.session = session;
    }

    @Override
    public FedoraEvent apply(final FedoraEvent evt) {
        final NamespaceRegistry namespaceRegistry = getNamespaceRegistry(session);

        final FedoraEvent event = new FedoraEvent(evt);
        for (final String property : evt.getProperties()) {
            final String[] parts = property.split(":", 2);
            if (parts.length == 2) {
                final String prefix = parts[0];
                if ("jcr".equals(prefix)) {
                    if (jcrProperties.contains(createProperty(REPOSITORY_NAMESPACE + parts[1]))) {
                        event.addProperty(REPOSITORY_NAMESPACE + parts[1]);
                    } else {
                        LOGGER.debug("Swallowing jcr property: {}", property);
                    }
                } else {
                    try {
                        event.addProperty(namespaceRegistry.getURI(prefix) + parts[1]);
                    } catch (final RepositoryException ex) {
                        LOGGER.debug("Prefix could not be dereferenced using the namespace registry: {}", property);
                        event.addProperty(property);
                    }
                }
            } else {
                event.addProperty(property);
            }
        }
        evt.getTypes().forEach(event::addType);
        return event;
    }

}
