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

import static java.lang.Long.parseLong;
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
    public static final String DEFAULT_TIMEOUT = Long.toString(ofMinutes(3).toMillis());

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.session.timeout";

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
       return ofMillis(parseLong(System.getProperty(TIMEOUT_SYSTEM_PROPERTY, DEFAULT_TIMEOUT)));
    }
}
