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
package org.fcrepo.kernel.impl.observer;

import com.google.common.base.Function;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.fcrepo.kernel.utils.NamespaceTools.getNamespaceRegistry;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Andrew Woods
 *         Date: 11/22/14
 */
public class GetNamespacedProperties implements Function<FedoraEvent, FedoraEvent> {

    private static final Logger LOGGER = getLogger(SimpleObserver.class);

    private Session session;

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
        for (String property : evt.getProperties()) {
            final String[] parts = property.split(":", 2);
            if (parts.length == 2) {
                final String prefix = parts[0];
                try {
                    event.addProperty(namespaceRegistry.getURI(prefix) + parts[1]);
                } catch (RepositoryException ex) {
                    LOGGER.trace("Prefix could not be dereferenced using the namespace registry: {}", property);
                    event.addProperty(property);
                }
            } else {
                event.addProperty(property);
            }
        }

        for (Integer type : evt.getTypes()) {
            event.addType(type);
        }
        return event;
    }

}
