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
package org.fcrepo.kernel.modeshape.observer.eventmappings;

import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static org.fcrepo.kernel.modeshape.observer.FedoraEventImpl.from;
import static org.fcrepo.kernel.modeshape.observer.FedoraEventImpl.getResourceTypes;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.jcr.observation.Event;

import org.fcrepo.kernel.api.observer.FedoraEvent;
import org.fcrepo.kernel.modeshape.observer.FedoraEventImpl;

import org.slf4j.Logger;

/**
 * Maps all JCR {@link Event}s concerning one JCR node to one {@link FedoraEvent}. Adds the types of those JCR events
 * together to calculate the final type of the emitted FedoraEvent.
 *
 * @author ajs6f
 * @author acoburn
 * @since Feb 27, 2014
 */
public class AllNodeEventsOneEvent implements InternalExternalEventMapper {

    private final static Logger LOGGER = getLogger(AllNodeEventsOneEvent.class);

    /**
     * Extracts an identifier from a JCR {@link Event} by building an id from nodepath and user to collapse multiple
     * events from repository mutations
     */
    private static final Function<Event, String> EXTRACT_NODE_ID = uncheck(ev -> {
            final FedoraEvent event = from(ev);
            final String id = event.getPath() + "-" + event.getUserID();
            LOGGER.debug("Sorting an event by identifier: {}", id);
            return id;
    });

    @Override
    public Stream<FedoraEvent> apply(final Stream<Event> events) {
        // first, index all the events by path-userID and then flatMap over that list of values
        // each of which returns either a singleton Stream or an empty Stream. The final result
        // will be a concatenated Stream of FedoraEvent objects.
        return events.collect(groupingBy(EXTRACT_NODE_ID)).entrySet().stream().flatMap(entry -> {
            final List<Event> evts = entry.getValue();
            if (!evts.isEmpty()) {
                // build a FedoraEvent from the first JCR Event
                final FedoraEvent fedoraEvent = from(evts.get(0));
                evts.stream().skip(1).forEach(evt -> {
                    // add types to the FedoraEvent from the subsequent JCR Events
                    fedoraEvent.getTypes().add(FedoraEventImpl.valueOf(evt.getType()));
                    fedoraEvent.getResourceTypes().addAll(getResourceTypes(evt).collect(toSet()));
                });
                return of(fedoraEvent);
            }
            return empty();
        });
    }
}
