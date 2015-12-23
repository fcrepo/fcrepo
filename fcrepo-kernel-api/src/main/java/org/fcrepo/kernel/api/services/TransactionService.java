/*
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.fcrepo.kernel.api.Transaction;

/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface TransactionService<AccessType> {

    /**
     * Check for expired transactions and remove them
     */
    void removeAndRollbackExpired();

    /**
     * Create a new Transaction and add it to the currently open ones
     *
     * @param access The access object to use for this Transaction
     * @param userName the user name
     * @return the {@link Transaction}
     */
    Transaction<AccessType> beginTransaction(AccessType access, String userName);

    /**
     * Receive an open {@link Transaction} for a given user
     *
     * @param txId the Id of the {@link Transaction}
     * @param userName the name  of the {@link java.security.Principal}
     * @return the {@link Transaction}
     * with this user
     */
    Transaction<AccessType> getTransaction(final String txId, final String userName);

    /**
     * Get the current Transaction for a session
     *
     * @param access an object providing access to the repository
     * @return transaction
     */
    Transaction<AccessType> getTransaction(AccessType access);

    /**
     * Check if a Transaction exists
     *
     * @param txid the Id of the {@link Transaction}
     * @return the {@link Transaction}
     */
    boolean exists(String txid);

    /**
     * Commit a {@link Transaction} with the given id
     *
     * @param txid the id of the {@link Transaction}
     * @return transaction
     */
    Transaction<AccessType> commit(String txid);

    /**
     * Roll a {@link Transaction} back
     *
     * @param txid the id of the {@link Transaction}
     * @return the {@link Transaction} object
     */
    Transaction<AccessType> rollback(String txid);

}
