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
package org.fcrepo.kernel.modeshape;

import static java.lang.Long.parseLong;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.modeshape.utils.NamespaceTools;

/**
 * An implementation of the FedoraSession abstraction
 * @author acoburn
 */
public class FedoraSessionImpl implements FedoraSession {

    // The default timeout is 3 minutes
    public static final String DEFAULT_TIMEOUT = Long.toString(ofMinutes(3).toMillis());

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.session.timeout";

    private final Session session;
    private final String id;
    private final Instant created;
    private final Map<String, String> sessionData;
    private Instant expires;

    /**
     * A key for looking up the transaction id in a session key-value pair
     */
    public static final String FCREPO_TX_ID = "fcrepo.tx.id";

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create a Fedora session with a JCR session and a username
     * @param session the JCR session
     */
    public FedoraSessionImpl(final Session session) {
        this.session = session;

        created = now();
        id = randomUUID().toString();
        expires = created.plus(operationTimeout());
        sessionData = new HashMap<>();
    }

    @Override
    public void commit() {
        try {
            if (session.isLive()) {
                final ObservationManager obs = session.getWorkspace().getObservationManager();
                final ObjectNode json = mapper.createObjectNode();
                sessionData.forEach(json::put);
                obs.setUserData(mapper.writeValueAsString(json));
                session.save();
            }
        } catch (final javax.jcr.AccessDeniedException ex) {
            throw new AccessDeniedException(ex);
        } catch (final RepositoryException | JsonProcessingException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

    @Override
    public void expire() {
        expires = now();
        try {
            if (session.isLive()) {
                session.refresh(false);
                session.logout();
            }
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

    @Override
    public Instant updateExpiry(final Duration amountToAdd) {
        if (session.isLive()) {
            expires = now().plus(amountToAdd);
        }
        return expires;
    }

    @Override
    public Instant getCreated() {
        return created;
    }

    @Override
    public Instant getExpires() {
        return expires;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUserId() {
        return session.getUserID();
    }

    @Override
    public Map<String, String> getNamespaces() {
        return NamespaceTools.getNamespaces(session);
    }

    @Override
    public void setSessionData(final String key, final String value) {
        sessionData.put(key, value);
    }

    @Override
    public Optional<String> getSessionData(final String key) {
        return ofNullable(sessionData.get(key));
    }

    /**
     * Get the internal JCR session
     * @return the internal JCR session
     */
    public Session getJcrSession() {
        return session;
    }

    /**
     * Get the internal JCR session from an existing FedoraSession
     * @param session the FedoraSession
     * @return the JCR session
     */
    public static Session getJcrSession(final FedoraSession session) {
        if (session instanceof FedoraSessionImpl) {
            return ((FedoraSessionImpl)session).getJcrSession();
        }
        throw new IllegalArgumentException("FedoraSession is of the wrong type");
    }

    /**
     * Retrieve the default operation timeout value
     * @return the default timeout value
     */
    public static Duration operationTimeout() {
       return ofMillis(parseLong(System.getProperty(TIMEOUT_SYSTEM_PROPERTY, DEFAULT_TIMEOUT)));
    }
}
