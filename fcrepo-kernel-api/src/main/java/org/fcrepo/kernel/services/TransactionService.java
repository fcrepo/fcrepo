/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.services;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.TransactionMissingException;

/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface TransactionService extends Service {

    /**
     * Check for expired transactions and remove them
     */
    void removeAndRollbackExpired();

    /**
     * Create a new Transaction and add it to the currently open ones
     *
     * @param sess The session to use for this Transaction
     * @return the {@link Transaction}
     */
    Transaction beginTransaction(Session sess, String userName) throws RepositoryException;

    /**
     * Recieve an open {@link Transaction} for a given user
     *
     * @param txId the Id of the {@link Transaction}
     * @param userName the name  of the {@link java.security.Principal}
     * @return the {@link Transaction}
     * @throws TransactionMissingException if the transaction could not be found or is not associated
     * with this user
     */
    Transaction getTransaction(final String txId, final String userName) throws TransactionMissingException;

    /**
     * Get the current Transaction for a session
     *
     * @param session
     * @return transaction
     * @throws TransactionMissingException
     */
    Transaction getTransaction(Session session) throws TransactionMissingException;

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
     * @throws RepositoryException
     */
    Transaction commit(String txid) throws RepositoryException;

    /**
     * Roll a {@link Transaction} back
     *
     * @param txid the id of the {@link Transaction}
     * @return the {@link Transaction} object
     * @throws RepositoryException if the {@link Transaction} could not be found
     */
    Transaction rollback(String txid) throws RepositoryException;

    /**
     * @param versionService the versionService to set
     */
    void setVersionService(final VersionService versionService);

}