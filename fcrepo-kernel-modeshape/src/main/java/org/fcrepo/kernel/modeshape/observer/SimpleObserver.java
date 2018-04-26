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
package org.fcrepo.kernel.modeshape.observer;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.collect.Iterators.filter;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_REPOSITORY_ROOT;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_RDF_SOURCE;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_DELETION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_RELOCATION;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NT_NAMESPACE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.MODE_NAMESPACE;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.slf4j.LoggerFactory.getLogger;

import  org.fcrepo.metrics.RegistryService;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.fcrepo.kernel.modeshape.observer.eventmappings.InternalExternalEventMapper;
import org.fcrepo.kernel.modeshape.utils.FedoraSessionUserUtil;

import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableSet;
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

    private static final Set<String> filteredNamespaces = ImmutableSet.of(
            JCR_NAMESPACE, MIX_NAMESPACE, JCR_NT_NAMESPACE, MODE_NAMESPACE);

    /**
     * A simple counter of events that pass through this observer
     */
    static final Counter EVENT_COUNTER =
            RegistryService.getInstance().getMetrics().counter(name(SimpleObserver.class, "onEvent"));

    static final Integer EVENT_TYPES = NODE_ADDED + NODE_REMOVED + NODE_MOVED + PROPERTY_ADDED + PROPERTY_CHANGED
            + PROPERTY_REMOVED;

    /**
     * Note: This function maps a FedoraEvent to a Stream of some number of FedoraEvents. This is because a MOVE event
     * may lead to an arbitrarily large number of additional events for any child resources. In the event of this not
     * being a MOVE event, the same FedoraEvent is returned, wrapped in a Stream. For a MOVEd resource, the resource in
     * question will be translated to two FedoraEvents: a MOVED event for the new resource location and a REMOVED event
     * corresponding to the old location. The same pair of FedoraEvents will also be generated for each child resource.
     */
    private static Function<FedoraEvent, Stream<FedoraEvent>> handleMoveEvents(final Session session) {
        return evt -> {
            if (evt.getTypes().contains(RESOURCE_RELOCATION)) {
                final Map<String, String> movePath = evt.getInfo();
                final String dest = movePath.get("destAbsPath");
                final String src = movePath.get("srcAbsPath");
                final FedoraSession fsession = new FedoraSessionImpl(session);

                try {
                    final FedoraResource resource = new FedoraResourceImpl(session.getNode(evt.getPath()));
                    return concat(of(evt), resource.getChildren(true).map(FedoraResource::getPath)
                        .flatMap(path -> of(
                            new FedoraEventImpl(RESOURCE_RELOCATION, path, evt.getResourceTypes(), evt.getUserID(),
                                fsession.getUserURI(), evt.getDate(), evt.getInfo()),
                            new FedoraEventImpl(RESOURCE_DELETION, path.replaceFirst(dest, src), evt.getResourceTypes(),
                                evt.getUserID(), fsession.getUserURI(), evt.getDate(), evt.getInfo()))));
                } catch (final RepositoryException ex) {
                    throw new RepositoryRuntimeException(ex);
                }
            }
            return of(evt);
        };
    }

    /**
     * Note: Certain RDF types are generated dynamically. These are added here, based on
     * certain type hints.
     */
    private static Function<String, Stream<String>> dynamicTypes = type -> {
        if (type.equals(ROOT)) {
            return of(FEDORA_REPOSITORY_ROOT, FEDORA_RESOURCE, FEDORA_CONTAINER, LDP_CONTAINER, LDP_RDF_SOURCE,
                    LDP_BASIC_CONTAINER);
        } else if (type.equals(FEDORA_CONTAINER)) {
            return of(FEDORA_CONTAINER, LDP_CONTAINER, LDP_RDF_SOURCE);
        } else if (type.equals(FEDORA_BINARY)) {
            return of(FEDORA_BINARY, LDP_NON_RDF_SOURCE);
        } else if (type.equals(FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
            return of(FEDORA_BINARY, LDP_NON_RDF_SOURCE);
        } else {
            return of(type);
        }
    };

    private static Function<FedoraEvent, FedoraEvent> filterAndDerefResourceTypes(final Session session) {
        final NamespaceRegistry registry = getNamespaceRegistry(session);
        return evt -> {
            final Set<String> resourceTypes = evt.getResourceTypes().stream()
                .flatMap(dynamicTypes).map(type -> type.split(":"))
                .filter(pair -> pair.length == 2).map(uncheck(pair -> new String[]{registry.getURI(pair[0]), pair[1]}))
                .filter(pair -> !filteredNamespaces.contains(pair[0])).map(pair -> pair[0] + pair[1]).collect(toSet());
            return new FedoraEventImpl(evt.getTypes(), evt.getPath(), resourceTypes, evt.getUserID(),
                    FedoraSessionUserUtil.getUserURI(evt.getUserID()), evt.getDate(), evt.getInfo());
        };
    }

    @Inject
    private FedoraRepository repository;

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
        session = getJcrSession(repository.login());
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
            lookupSession = getJcrSession(repository.login());

            @SuppressWarnings("unchecked")
            final Iterator<Event> filteredEvents = filter(events, eventFilter::test);
            eventMapper.apply(iteratorToStream(filteredEvents))
                .map(filterAndDerefResourceTypes(lookupSession))
                .flatMap(handleMoveEvents(lookupSession))
                .forEach(this::post);
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
