/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.observer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.fcrepo.config.AuthPropsConfig;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.operations.ResourceOperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.EventBus;

/**
 * @author pwinckles
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class EventAccumulatorImpl implements EventAccumulator {

    private final static Logger LOG = LoggerFactory.getLogger(EventAccumulatorImpl.class);

    private final Map<String, Multimap<FedoraId, EventBuilder>> transactionEventMap;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private EventBus eventBus;

    @Inject
    private AuthPropsConfig authPropsConfig;

    public EventAccumulatorImpl() {
        this.transactionEventMap = new ConcurrentHashMap<>();
    }

    @Override
    public void recordEventForOperation(final Transaction transaction, final FedoraId fedoraId,
                                        final ResourceOperation operation) {
        checkNotNull(transaction, "transaction cannot be blank");
        checkNotNull(fedoraId, "fedoraId cannot be null");

        final String transactionId = transaction.getId();
        final var events = transactionEventMap.computeIfAbsent(transactionId, key ->
                MultimapBuilder.hashKeys().arrayListValues().build());
        final var eventBuilder = ResourceOperationEventBuilder.fromResourceOperation(
                fedoraId, operation, authPropsConfig.getUserAgentBaseUri());
        events.put(fedoraId, eventBuilder);
    }

    @Override
    public void emitEvents(final Transaction transaction, final String baseUrl, final String userAgent) {
        LOG.debug("Emitting events for transaction {}", transaction.getId());

        final var eventMap = transactionEventMap.remove(transaction.getId());

        if (eventMap != null) {
            eventMap.keySet().forEach(fedoraId -> {
                final var events = eventMap.get(fedoraId);

                try {
                    final var mergedBuilder = events.stream()
                            .reduce(EventBuilder::merge).get();

                    final var event = mergedBuilder
                            .withResourceTypes(loadResourceTypes(transaction, fedoraId))
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
    public void clearEvents(final Transaction transaction) {
        LOG.trace("Clearing events for transaction {}", transaction.getId());
        transactionEventMap.remove(transaction.getId());
    }

    private Set<String> loadResourceTypes(final Transaction transaction, final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(transaction, fedoraId).getTypes().stream()
                    .map(URI::toString)
                    .collect(Collectors.toSet());
        } catch (final Exception e) {
            LOG.debug("Could not load resource types for {}", fedoraId, e);
            // This can happen if the resource no longer exists
            return Collections.emptySet();
        }
    }

}
