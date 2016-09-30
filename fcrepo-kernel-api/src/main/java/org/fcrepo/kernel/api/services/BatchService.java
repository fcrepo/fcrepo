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
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.FedoraSession;

/**
 * @author acoburn
 * @since Sept 30, 2016
 */
public interface BatchService {

    /**
     * Check for expired batch operations and remove them
     */
    void removeExpired();

    /**
     * Create a new batch operation with a FedoraSession and add it to the currently open ones
     *
     * @param session The session to use for this batch operation
     * @param username the name of the {@link java.security.Principal}
     */
    void begin(FedoraSession session, String username);

    /**
     * Create a new FedoraSession for the anonymous user and add it to the currently open ones
     *
     * @param session The session to use for this batch operation
     */
    default void begin(FedoraSession session) {
        begin(session, null);
    }

    /**
     * Retrieve an open {@link FedoraSession} for a given user
     *
     * @param txId the Id of the {@link FedoraSession}
     * @param username the name of the {@link java.security.Principal}
     * @return the {@link FedoraSession} with this user
     */
    FedoraSession getSession(String txId, String username);

    /**
     * Retrieve an open {@link FedoraSession} for an anonymous user
     *
     * @param txId the Id of the {@link FedoraSession}
     * @return the {@link FedoraSession}
     */
    default FedoraSession getSession(String txId) {
        return getSession(txId, null);
    }

    /**
     * Check if a FedoraSession exists for a particular user
     *
     * @param txid the Id of the {@link FedoraSession}
     * @param username the name of the {@link java.security.Principal}
     * @return the {@link FedoraSession} object for the defined user
     */
    boolean exists(String txid, String username);

    /**
     * Check if a FedoraSession exists for the anonymous user
     *
     * @param txid the Id of the {@link FedoraSession}
     * @return the {@link FedoraSession} object
     */
    default boolean exists(String txid) {
        return exists(txid, null);
    }

    /**
     * Commit any changes during a {@link FedoraSession} with the given id and username
     *
     * @param txid the id of the {@link FedoraSession}
     * @param username the name of the {@link java.security.Principal}
     */
    void commit(String txid, String username);

    /**
     * Commit any changes during a {@link FedoraSession} with the given id for the anonymous user
     *
     * @param txid the id of the {@link FedoraSession}
     */
    default void commit(String txid) {
        commit(txid, null);
    }

    /**
     * Roll back any uncommited changes during a {@link FedoraSession}
     *
     * @param txid the id of the {@link FedoraSession}
     * @param username the name of the {@link java.security.Principal}
     */
    void abort(String txid, String username);

    /**
     * Roll back any uncommited changes during a {@link FedoraSession} for the anonymous user
     *
     * @param txid the id of the {@link FedoraSession}
     */
    default void abort(String txid) {
        abort(txid, null);
    }
}
