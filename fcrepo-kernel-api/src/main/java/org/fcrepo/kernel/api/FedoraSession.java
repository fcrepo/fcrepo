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
package org.fcrepo.kernel.api;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofMillis;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * The Fedora Session abstraction
 *
 * @author acoburn
 */
public interface FedoraSession {

    // The default timeout is 3 minutes
    public static final Duration DEFAULT_TIMEOUT = ofMinutes(3);

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.transactions.timeout";

    public interface State {}

    public enum RequiredState implements State {
        BATCH, EXPIRED, ACTIVE
    }

    /**
     * Begin a BATCH session
     */
    void beginBatch();

    /**
     * Expire the session
     */
    void expire();

    /**
     * Commit any batch operations
     */
    void commit();

    /**
     * Rollback any batch operations
     */
    void rollback();

    /**
     * Update the expiry by the provided amount
     * @param amountToAdd the amount of time to add
     * @return the new expiration date
     */
    Instant updateExpiry(Duration amountToAdd);

    /**
     * Determine whether the session has the provided state
     * @param <T> an enum that extends the default State enum
     * @param state the state to test
     * @return whether the session is in the given state
     */
    <T extends State> boolean hasState(T state);

    /**
     * Get the date this session was created
     * @return creation date
     */
    Instant getCreated();

    /**
     * Get the date this session expires
     * @return expiration date
     */
    Instant getExpires();

    /**
     * Get the session identifier
     * @return the session id
     */
    String getId();

    /**
     * Get the user identifier associated with this session
     * @return the user id
     */
    String getUserId();

    /**
     * Set a namespace prefix
     * @param prefix the prefix
     * @param uri the URI
     */
    void setNamespacePrefix(String prefix, String uri);

    /**
     * Get a namespace for a given prefix.
     * @param prefix the prefix
     * @return the namespace URI
     */
    Optional<String> getNamespace(String prefix);

    /**
     * Get a mapping of registered namespaces
     * @return the namespace mapping
     */
    Map<String, String> getNamespaces();

    /**
     * Set session-specific data
     * @param key the key
     * @param value the value
     */
    void setSessionData(String key, String value);

    /**
     * Retrieve the session data for a given key
     * @param key the key
     * @return the value
     */
    Optional<String> getSessionData(String key);

    /**
     * Retrieve the default operation timeout value
     * @return the default timeout value
     */
    public static Duration operationTimeout() {
       if (System.getProperty(TIMEOUT_SYSTEM_PROPERTY) != null) {
            return ofMillis(Long.parseLong(System.getProperty(TIMEOUT_SYSTEM_PROPERTY)));
        } else {
            return DEFAULT_TIMEOUT;
        }
    }
}
