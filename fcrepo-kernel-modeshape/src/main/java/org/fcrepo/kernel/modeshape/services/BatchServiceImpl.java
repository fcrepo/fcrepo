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
/**
 *
 */

package org.fcrepo.kernel.modeshape.services;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toSet;
import static com.google.common.base.Strings.nullToEmpty;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.operationTimeout;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.SessionMissingException;
import org.fcrepo.kernel.api.services.BatchService;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This is part of the strawman implementation for Fedora batch operations This
 * service implements a simple {@link FedoraSession} service which is able to
 * create/commit/rollback {@link FedoraSession} objects. A {@link Scheduled}
 * annotation is used for removing timed out operations
 *
 * @author frank asseg
 * @author ajs6f
 * @author acoburn
 */
@Component
public class BatchServiceImpl extends AbstractService implements BatchService {

    private static final Logger LOGGER = getLogger(BatchServiceImpl.class);

    /**
     * TODO since sessions have to be available on all nodes, they have to
     * be either persisted or written to a distributed map or sth, not just this
     * plain hashmap that follows
     */
    private static Map<String, FedoraSession> sessions = new ConcurrentHashMap<>();

    @VisibleForTesting
    public static final long REAP_INTERVAL = 1000;

    /**
     * Every REAP_INTERVAL milliseconds, check for expired sessions. If the
     * tx is expired, roll it back and remove it from the registry.
     */
    @Override
    @Scheduled(fixedRate = REAP_INTERVAL)
    public void removeExpired() {
        final Set<String> reapable = sessions.entrySet().stream()
                .filter(e -> e.getValue().getExpires().isPresent())
                .filter(e -> e.getValue().getExpires().get().isBefore(now()))
                .map(Map.Entry::getKey).collect(toSet());
        reapable.forEach(key -> {
            final FedoraSession s = sessions.get(key);
            if (s != null) {
                try {
                    s.expire();
                } catch (final RepositoryRuntimeException e) {
                    LOGGER.error("Got exception rolling back expired session {}: {}", s, e.getMessage());
                }
            }
            sessions.remove(key);
        });
    }

    @Override
    public void begin(final FedoraSession session, final String username) {
        sessions.put(getTxKey(session.getId(), username), session);
    }

    @Override
    public FedoraSession getSession(final String sessionId, final String username) {
        final FedoraSession session = sessions.get(getTxKey(sessionId, username));
        if (session == null) {
            throw new SessionMissingException("Batch session with id: " + sessionId + " is not available");
        }
        return session;
    }

    @Override
    public boolean exists(final String sessionId, final String username) {
        return sessions.containsKey(getTxKey(sessionId, username));
    }

    @Override
    public void commit(final String sessionId, final String username) {
        final FedoraSession session = getSession(sessionId, username);
        session.commit();
        sessions.remove(getTxKey(sessionId, username));
    }

    @Override
    public void refresh(final String sessionId, final String username) {
        final FedoraSession session = getSession(sessionId, username);
        session.updateExpiry(operationTimeout());
    }

    @Override
    public void abort(final String sessionId, final String username) {
        final FedoraSession session = getSession(sessionId, username);
        session.expire();
        sessions.remove(getTxKey(sessionId, username));
    }

    private static String getTxKey(final String sessionId, final String username) {
        return nullToEmpty(username) + ":" + sessionId;
    }
}
