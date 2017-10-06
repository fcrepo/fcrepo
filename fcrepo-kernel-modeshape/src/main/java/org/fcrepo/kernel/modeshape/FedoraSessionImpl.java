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
import static java.util.Collections.singleton;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.modeshape.utils.FedoraSessionUserUtil;
import org.fcrepo.kernel.modeshape.utils.NamespaceTools;

/**
 * An implementation of the FedoraSession abstraction
 * @author acoburn
 */
public class FedoraSessionImpl implements FedoraSession {
    // The default timeout is 3 minutes
    @VisibleForTesting
    public static final String DEFAULT_TIMEOUT = Long.toString(ofMinutes(3).toMillis());

    @VisibleForTesting
    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.session.timeout";

    private final Session jcrSession;
    private final String id;
    private final Instant created;
    private final ConcurrentHashMap<String, String> sessionData;
    private Instant expires;

    /**
     * A key for looking up the transaction id in a session key-value pair
     */
    public static final String FCREPO_TX_ID = "fcrepo.tx.id";

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create a Fedora session with a JCR session
     * @param session the JCR session
     */
    public FedoraSessionImpl(final Session session) {
        this.jcrSession = session;

        created = now();
        id = randomUUID().toString();
        expires = created.plus(operationTimeout());
        sessionData = new ConcurrentHashMap<>();
    }

    @Override
    public void commit() {
        try {
            if (jcrSession.isLive()) {
                final ObservationManager obs = jcrSession.getWorkspace().getObservationManager();
                final ObjectNode json = mapper.createObjectNode();
                sessionData.forEach(json::put);
                obs.setUserData(mapper.writeValueAsString(json));
                jcrSession.save();
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
            if (jcrSession.isLive()) {
                jcrSession.refresh(false);
                jcrSession.logout();
            }
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

    @Override
    public Instant updateExpiry(final Duration amountToAdd) {
        if (jcrSession.isLive()) {
            expires = now().plus(amountToAdd);
        }
        return expires;
    }

    @Override
    public Instant getCreated() {
        return created;
    }

    @Override
    public Optional<Instant> getExpires() {
        return of(expires);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public URI getUserURI() {
        return FedoraSessionUserUtil.getUserURI(jcrSession.getUserID());
    }

    @Override
    public Map<String, String> getNamespaces() {
        return NamespaceTools.getNamespaces(jcrSession);
    }

    /**
     *  Add session data
     *  @param key the data key
     *  @param value the data value
     *
     *  Note: while the FedoraSession interface permits multi-valued
     *  session data, this implementation constrains that to be single-valued.
     *  That is, calling obj.addSessionData("key", "value1") followed by
     *  obj.addSessionData("key", "value2") will result in only "value2" being associated
     *  with the given key.
     */
    @Override
    public void addSessionData(final String key, final String value) {
        sessionData.put(key, value);
    }

    @Override
    public Collection<String> getSessionData(final String key) {
        return singleton(sessionData.get(key));
    }

    @Override
    public void removeSessionData(final String key, final String value) {
        sessionData.remove(key, value);
    }

    @Override
    public void removeSessionData(final String key) {
        sessionData.remove(key);
    }

    /**
     * Get the internal JCR session
     * @return the internal JCR session
     */
    public Session getJcrSession() {
        return jcrSession;
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
        throw new ClassCastException("FedoraSession is not a " + FedoraSessionImpl.class.getCanonicalName());
    }

    /**
     * Retrieve the default operation timeout value
     * @return the default timeout value
     */
    public static Duration operationTimeout() {
       return ofMillis(parseLong(System.getProperty(TIMEOUT_SYSTEM_PROPERTY, DEFAULT_TIMEOUT)));
    }
}
