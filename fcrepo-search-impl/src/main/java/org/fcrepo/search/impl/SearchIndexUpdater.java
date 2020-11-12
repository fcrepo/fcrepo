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


package org.fcrepo.search.impl;

import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.fcrepo.kernel.api.models.ResourceHelper;
import org.fcrepo.kernel.api.observer.Event;
import org.fcrepo.kernel.api.observer.EventType;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.search.api.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Set;

import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_CREATION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_DELETION;
import static org.fcrepo.kernel.api.observer.EventType.RESOURCE_MODIFICATION;

/**
 * This class listens to events from the event bus and updates the search
 * index accordingly.
 *
 * @author dbernstein
 */
@Component
public class SearchIndexUpdater {

    @Inject
    private EventBus eventBus;

    @Autowired
    @Qualifier("searchIndex")
    private SearchIndex searchIndex;

    @Inject
    private ResourceHelper resourceHelper;

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    private static Logger LOGGER = LoggerFactory.getLogger(SearchIndexUpdater.class);
    private static final Set<EventType> HANDLED_TYPES = Sets.newHashSet(RESOURCE_CREATION, RESOURCE_MODIFICATION,
            RESOURCE_DELETION);

    @Subscribe
    @AllowConcurrentEvents
    public void onEvent(final Event event) {
        LOGGER.debug("event={}", event);
        try {
            final var fedoraId = event.getFedoraId();
            final var types = event.getTypes();
            if (types.contains(RESOURCE_DELETION) && !resourceHelper.doesResourceExist(null, fedoraId, false)) {
                this.searchIndex.removeFromIndex(fedoraId);
            } else if (types.contains(RESOURCE_CREATION) || types.contains(RESOURCE_MODIFICATION)) {
                final var session = persistentStorageSessionManager.getReadOnlySession();
                final var headers = session.getHeaders(fedoraId, null);
                this.searchIndex.addUpdateIndex(headers);
            }
        } catch (final PersistentStorageException e) {
            LOGGER.error("Failed to handle event: " + event, e);
        }
    }

    /**
     * Register listener
     */
    @PostConstruct
    public void register() {
        LOGGER.debug("Registering: {}", this.getClass().getCanonicalName());
        eventBus.register(this);
    }

    /**
     * Unregister listener
     */
    @PreDestroy
    public void releaseConnections() {
        LOGGER.debug("Unregistering: {}", this.getClass().getCanonicalName());
        eventBus.unregister(this);
    }
}
