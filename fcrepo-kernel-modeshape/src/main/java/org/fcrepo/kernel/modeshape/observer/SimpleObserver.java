/*
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

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.collect.Iterators.filter;
import static java.util.Collections.emptyMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIED;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isPublicJcrProperty;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.slf4j.LoggerFactory.getLogger;

import  org.fcrepo.metrics.RegistryService;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.observer.eventmappings.InternalExternalEventMapper;

import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.eventbus.EventBus;

/**
 * Simple JCR EventListener that filters JCR Events through a Fedora EventFilter, maps the results through a mapper,
 * and puts the resulting stream onto the internal Fedora EventBus as a stream of FedoraEvents.
 *
 * @author eddies
 * @author ajs6f
 * @since Feb 7, 2013
 */
public class SimpleObserver implements EventListener {

    private static final Logger LOGGER = getLogger(SimpleObserver.class);

    /**
     * A simple counter of events that pass through this observer
     */
    static final Counter EVENT_COUNTER =
            RegistryService.getInstance().getMetrics().counter(name(SimpleObserver.class, "onEvent"));

    static final Integer EVENT_TYPES = NODE_ADDED + NODE_REMOVED + NODE_MOVED + PROPERTY_ADDED + PROPERTY_CHANGED
            + PROPERTY_REMOVED;

    /**
     * Note: In order to resolve the JCR-based properties (e.g. jcr:lastModified or
     * ebucore:mimeType) a new FedoarEvent is created and the dereferenced properties
     * are added to that new event.
     */
    private static Function<Session, Function<FedoraEvent, FedoraEvent>> namespaceResolver = session -> evt -> {
        final NamespaceRegistry namespaceRegistry = getNamespaceRegistry(session);
        final FedoraEvent event = new FedoraEventImpl(evt.getTypes(), evt.getPath(), evt.getUserID(),
                evt.getUserData(), evt.getDate(), evt.getInfo());

        evt.getProperties().stream()
            .filter(isPublicJcrProperty.or(property -> !property.startsWith("jcr:")))
            .forEach(property -> {
                final String[] parts = property.split(":", 2);
                if (parts.length == 2) {
                    try {
                        event.addProperty(
                            getRDFNamespaceForJcrNamespace(namespaceRegistry.getURI(parts[0])) + parts[1]);
                    } catch (final RepositoryException ex) {
                        LOGGER.warn("Prefix could not be dereferenced using the namespace registry, skipping: {}",
                            property);
                    }
                } else {
                    LOGGER.warn("Prefix could not be determined, skipping: {}", property);
                }
            });
        if (evt.getProperties().contains(JCR_LASTMODIFIED) && !evt.getProperties().contains(FEDORA_LASTMODIFIED)) {
            event.addProperty(REPOSITORY_NAMESPACE + "lastModified");
        }
        return event;
    };

    /**
     * Note: This function maps a FedoraEvent to a Stream of some number of FedoraEvents. This is because a MOVE event
     * may lead to an arbitrarily large number of additional events for any child resources. In the event of this not
     * being a MOVE event, the same FedoraEvent is returned, wrapped in a Stream. For a MOVEd resource, the resource in
     * question will be translated to two FedoraEvents: a MOVED event for the new resource location and a REMOVED event
     * corresponding to the old location. The same pair of FedoraEvents will also be generated for each child resource.
     */
    private static Function<Session, Function<FedoraEvent, Stream<FedoraEvent>>> handleMoveEvents = session -> evt -> {
        if (evt.getTypes().contains(EventType.NODE_MOVED)) {
            final Map<String, String> movePath = evt.getInfo();
            final String dest = movePath.get("destAbsPath");
            final String src = movePath.get("srcAbsPath");
            try {
                final FedoraResource resource = new FedoraResourceImpl(session.getNode(evt.getPath()));
                return concat(of(evt), resource.getChildren(true).map(FedoraResource::getPath)
                    .flatMap(path -> of(
                        new FedoraEventImpl(EventType.NODE_MOVED, path, evt.getUserID(),
                            evt.getUserData(), evt.getDate(), emptyMap()),
                        new FedoraEventImpl(EventType.NODE_REMOVED, path.replaceFirst(dest, src),
                            evt.getUserID(), evt.getUserData(), evt.getDate(), emptyMap()))));
            } catch (final RepositoryException ex) {
                throw new RepositoryRuntimeException(ex);
            }
        }
        return of(evt);
    };

    @Inject
    private Repository repository;

    @Inject
    private EventBus eventBus;

    @Inject
    private InternalExternalEventMapper eventMapper;

    @Inject
    private EventFilter eventFilter;

    // THIS SESSION SHOULD NOT BE USED TO LOOK UP NODES
    // it is used only to register and deregister this observer to the JCR
    private Session session;

    /**
     * Register this observer with the JCR event listeners
     *
     * @throws RepositoryException if repository exception occurred
     */
    @PostConstruct
    public void buildListener() throws RepositoryException {
        LOGGER.debug("Constructing an observer for JCR events...");
        session = repository.login();
        session.getWorkspace().getObservationManager()
                .addEventListener(this, EVENT_TYPES, "/", true, null, null, false);
        session.save();
    }

    /**
     * logout of the session
     *
     * @throws RepositoryException if repository exception occurred
     */
    @PreDestroy
    public void stopListening() throws RepositoryException {
        try {
            LOGGER.debug("Destroying an observer for JCR events...");
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } finally {
            session.logout();
        }
    }

    /**
     * Filter JCR events and transform them into our own FedoraEvents.
     *
     * @param events the JCR events
     */
    @Override
    public void onEvent(final javax.jcr.observation.EventIterator events) {
        Session lookupSession = null;
        try {
            lookupSession = repository.login();
            final Function<FedoraEvent, Stream<FedoraEvent>> moveEventHandler = handleMoveEvents.apply(lookupSession);

            @SuppressWarnings("unchecked")
            final Iterator<Event> filteredEvents = filter(events, eventFilter::test);
            eventMapper.apply(iteratorToStream(filteredEvents))
                .flatMap(moveEventHandler)
                .map(namespaceResolver.apply(lookupSession))
                .forEach(this::post);
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        } finally {
            if (lookupSession != null) {
                lookupSession.logout();
            }
        }
    }

    private void post(final FedoraEvent evt) {
        eventBus.post(evt);
        EVENT_COUNTER.inc();
    }
}
