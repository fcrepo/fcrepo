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

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * The Fedora Session abstraction
 *
 * @author acoburn
 */
public interface FedoraSession {

    /**
     * Expire the session
     */
    void expire();

    /**
     * Commit any batch operations
     */
    void commit();

    /**
     * Update the expiry by the provided amount
     * @param amountToAdd the amount of time to add
     * @return the new expiration date
     */
    Instant updateExpiry(Duration amountToAdd);

    /**
     * Get the date this session was created
     * @return creation date
     */
    Instant getCreated();

    /**
     * Get the date this session expires
     * @return expiration date, if one exists
     */
    Optional<Instant> getExpires();

    /**
     * Get the session identifier
     * @return the session id
     */
    String getId();

    /**
     * Get the user URI
     * @return URI
     */
    URI getUserURI();

    /**
     * Get a mapping of registered namespaces
     * @return the namespace mapping
     */
    Map<String, String> getNamespaces();

    /**
     * Add session-specific data
     * @param key the key
     * @param value the value
     *
     * Note: it is up to the particular implementation as to whether multi-valued session data
     * is allowed.
     */
    void addSessionData(String key, String value);

    /**
     * Retrieve the session data for a given key
     * @param key the key
     * @return the value
     */
    Collection<String> getSessionData(String key);

    /**
     * Remove a particular session key-value pair
     * @param key the data key
     * @param value the data value
     */
    void removeSessionData(String key, String value);

    /**
     * Remove all session data for a particular key
     * @param key the data key
     */
    default void removeSessionData(String key) {
        getSessionData(key).forEach(v -> removeSessionData(key, v));
    }
}
