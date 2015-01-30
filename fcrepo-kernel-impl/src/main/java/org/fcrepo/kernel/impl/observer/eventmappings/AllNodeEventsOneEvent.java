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
package org.fcrepo.kernel.impl.observer.eventmappings;

import static com.google.common.collect.Multimaps.index;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.Arrays.asList;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;

import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.observer.FedoraEvent;
import org.fcrepo.kernel.observer.eventmappings.InternalExternalEventMapper;

import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;

/**
 * Maps all JCR {@link Event}s concerning one JCR node to one
 * {@link FedoraEvent}. Adds the types of those JCR events together to calculate
 * the final type of the emitted FedoraEvent. TODO stop aggregating events in
 * the heap and make this a purely iterative algorithm, if possible
 *
 * @author ajs6f
 * @since Feb 27, 2014
 */
public class AllNodeEventsOneEvent implements InternalExternalEventMapper {

    private static final List<Integer> PROPERTY_EVENT_TYPES = asList(PROPERTY_ADDED, PROPERTY_CHANGED,
            PROPERTY_REMOVED);

    /**
     * Extracts the node identifier from a JCR {@link Event}.
     */
    private static final Function<Event, String> EXTRACT_NODE_ID = new Function<Event, String>() {

        @Override
        public String apply(final Event ev) {
            try {
                final String id = ev.getIdentifier();
                log.debug("Sorting an event by identifier: {}", id);
                return id;
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
        }
    };

    @Override
    public Iterator<FedoraEvent> apply(final Iterator<Event> events) {

        return new Iterator<FedoraEvent>() {

            // sort JCR events into a Multimap keyed by the node ID involved
            final Multimap<String, Event> sortedEvents = index(events, EXTRACT_NODE_ID);

            final Iterator<String> nodeIds = sortedEvents.keySet().iterator();

            @Override
            public boolean hasNext() {
                return nodeIds.hasNext();
            }

            @Override
            public FedoraEvent next() {
                final Iterator<Event> nodeSpecificEvents = sortedEvents.get(nodeIds.next()).iterator();
                // we can safely call next() immediately on nodeSpecificEvents
                // because if
                // there was no event at all, there would appear no entry in our
                // Multimap under this key
                final Event firstEvent = nodeSpecificEvents.next();
                final FedoraEvent fedoraEvent = new FedoraEvent(firstEvent);

                addProperty(fedoraEvent, firstEvent);
                while (nodeSpecificEvents.hasNext()) {
                    // add the event type and property name to the event we are building up to emit
                    // we could aggregate other information here if that seems useful
                    final Event otherEvent = nodeSpecificEvents.next();
                    fedoraEvent.addType(otherEvent.getType());
                    addProperty(fedoraEvent, otherEvent);
                }
                return fedoraEvent;
            }

            @Override
            public void remove() {
                // the underlying Multimap is immutable anyway
                throw new UnsupportedOperationException();
            }

            private void addProperty( final FedoraEvent fedoraEvent, final Event ev ) {
                try {
                    if (PROPERTY_EVENT_TYPES.contains(ev.getType())) {
                        final String eventPath = ev.getPath();
                        fedoraEvent.addProperty(eventPath.substring(eventPath.lastIndexOf("/") + 1));
                    } else {
                        log.trace("Not adding non-event property: {}, {}", fedoraEvent, ev);
                    }
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        };
    }

    private final static Logger log = getLogger(AllNodeEventsOneEvent.class);
}
