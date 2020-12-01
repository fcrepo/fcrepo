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

import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import java.time.Duration;
import java.time.Instant;

/**
 * The Fedora Transaction abstraction
 *
 * @author mohideen
 */
public interface Transaction {

    /**
     * Commit the transaction
     */
    void commit();

    /**
     * Commit the transaction only if the transaction is shortLived
     */
    void commitIfShortLived();

    /**
     * @return returns true if this transaction has already been committed
     */
    boolean isCommitted();

    /**
     * Rollback the transaction
     */
    void rollback();

    /**
     * @return true if this transaction has been rolled back
     */
    boolean isRolledBack();

    /**
     * Get the transaction id
     *
     * @return the transaction id.
     */
    String getId();

    /**
     * Check if the transaction is short-lived.
     *
     * @return is the transaction short-lived.
     */
    boolean isShortLived();

    /**
     * Set transaction short-lived state.
     *
     * @param shortLived boolean true (short-lived) or false (not short-lived)
     */
    void setShortLived(boolean shortLived);

    /**
     * Expire a transaction
     */
    void expire();

    /**
     * Has the transaction expired?
     * @return true if expired
     */
    boolean hasExpired();

    /**
    * Update the expiry by the provided amount
    * @param amountToAdd the amount of time to add
    * @return the new expiration date
    */
    Instant updateExpiry(Duration amountToAdd);

    /**
     * Get the date this session expires
     * @return expiration date, if one exists
     */
    Instant getExpires();

    /**
     * Refresh the transaction to extend its expiration window.
     */
    void refresh();

    /**
     * Acquires a lock on the specified resource for this transaction.
     *
     * @param resourceId the resource to lock
     * @throws ConcurrentUpdateException if the lock cannot be acquired
     */
    void lockResource(final FedoraId resourceId);

    /**
     * Releases any resource locks held by the transaction if the session is short-lived. This method should always be
     * called after handling a request, regardless of the outcome, so that any held locks are released immediately
     * without having to wait for the short-lived transaction to expire.
     */
    void releaseResourceLocksIfShortLived();

    /**
     * Sets the baseUri on the transaction
     * @param baseUri the baseUri of the requests
     */
    void setBaseUri(String baseUri);

    /**
     * Sets the user-agent on the transaction
     * @param userAgent the request's user-agent
     */
    void setUserAgent(String userAgent);

}
