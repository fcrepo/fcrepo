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

package org.fcrepo.kernel.impl.observer;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.EventBus;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

/**
 * @author pwinckles
 */
@Component
public class EventAccumulatorImpl implements EventAccumulator {

    private final static Logger LOG = LoggerFactory.getLogger(EventAccumulatorImpl.class);

    private final Map<String, Multimap<FedoraId, EventBuilder>> transactionEventMap;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private EventBus eventBus;

    public EventAccumulatorImpl() {
        this.transactionEventMap = new ConcurrentHashMap<>();
    }

    @Override
    public void recordEventForOperation(final String transactionId, final FedoraId fedoraId,
                                        final ResourceOperation operation) {
        checkNotNull(emptyToNull(transactionId), "transactionId cannot be blank");
        checkNotNull(fedoraId, "fedoraId cannot be null");

        final var events = transactionEventMap.computeIfAbsent(transactionId, key ->
                MultimapBuilder.hashKeys().arrayListValues().build());
        final var eventBuilder = ResourceOperationEventBuilder.fromResourceOperation(fedoraId, operation);
        events.put(fedoraId, eventBuilder);
    }

    @Override
    public void emitEvents(final String transactionId, final String baseUrl, final String userAgent) {
        LOG.debug("Emitting events for transaction {}", transactionId);

        final var eventMap = transactionEventMap.remove(transactionId);

        if (eventMap != null) {
            eventMap.keySet().forEach(fedoraId -> {
                final var events = eventMap.get(fedoraId);

                try {
                    final var mergedBuilder = events.stream()
                            .reduce(EventBuilder::merge).get();

                    final var event = mergedBuilder
                            .withResourceTypes(loadResourceTypes(fedoraId))
                            .withBaseUrl(baseUrl)
                            .withUserAgent(userAgent)
                            .build();

                    LOG.debug("Emitting event: {}", event);
                    eventBus.post(event);
                } catch (final Exception e) {
                    LOG.error("Failed to emit events: {}", events, e);
                }
            });
        }
    }

    @Override
    public void clearEvents(final String transactionId) {
        LOG.trace("Clearing events for transaction {}", transactionId);
        transactionEventMap.remove(transactionId);
    }

    private Set<String> loadResourceTypes(final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(fedoraId).getTypes().stream()
                    .map(URI::toString)
                    .collect(Collectors.toSet());
        } catch (final Exception e) {
            LOG.debug("Could not load resource types for {}", fedoraId, e);
            // This can happen if the resource no longer exists
            return Collections.emptySet();
        }
    }

}
