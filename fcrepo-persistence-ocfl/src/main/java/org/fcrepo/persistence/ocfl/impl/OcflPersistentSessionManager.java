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
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OCFL implementation of PersistentStorageSessionManager
 *
 * @author whikloj
 * @author dbernstein
 * @since 2019-09-20
 */
@Component
public class OcflPersistentSessionManager implements PersistentStorageSessionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcflPersistentSessionManager.class);

    private volatile PersistentStorageSession readOnlySession;

    private Map<String, PersistentStorageSession> sessionMap;

    @Inject
    private OcflObjectSessionFactory objectSessionFactory;

    @Inject
    private FedoraToOcflObjectIndex ocflIndex;

    @Inject
    private ReindexService reindexService;

    /**
     * Default constructor
     */
    @Autowired
    public OcflPersistentSessionManager() {
        this.sessionMap = new ConcurrentHashMap<>();
    }

    @Override
    public PersistentStorageSession getSession(final String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("session id must be non-null");
        }

        return sessionMap.computeIfAbsent(sessionId, key -> {
            LOGGER.debug("Creating storage session {}", sessionId);
            return new OcflPersistentStorageSessionMetrics(
                    new OcflPersistentStorageSession(
                            key,
                            ocflIndex,
                            objectSessionFactory,
                            reindexService));
        });
    }

    @Override
    public PersistentStorageSession getReadOnlySession() {
        var localSession = this.readOnlySession;

        if (localSession == null) {
            synchronized (this) {
                localSession = this.readOnlySession;
                if (localSession == null) {
                    this.readOnlySession = new OcflPersistentStorageSessionMetrics(
                            new OcflPersistentStorageSession(ocflIndex, objectSessionFactory, reindexService));
                    localSession = this.readOnlySession;
                }
            }
        }

        return localSession;
    }

    @Override
    public PersistentStorageSession removeSession(final String sessionId) {
        LOGGER.debug("Removing storage session {}", sessionId);
        return sessionMap.remove(sessionId);
    }
}
