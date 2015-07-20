/**
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
package org.fcrepo.kernel.api;

import java.util.Date;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author bbpennel
 * @since Feb 18, 2014
 */
public interface Transaction {

    public static enum State {
        DIRTY, NEW, COMMITED, ROLLED_BACK
    }

    /**
     * Get the transaction-aware session
     * @return transaction-aware session
     */
    Session getSession();

    /**
     * Get the date this transaction was created
     * @return creation date
     */
    Date getCreated();

    /**
     * Get the transaction identifier
     * @return transaction id
     */
    String getId();

    /**
     * Get the state of this transaction
     * @return transaction state
     * @throws RepositoryException if repository exception occurred
     */
    State getState() throws RepositoryException;

    /**
     * Get the Date when this transaction is expired and can be
     * garbage-collected
     * @return transaction expiration date
     */
    Date getExpires();

    /**
     * "Commit" the transaction by saving the backing-session
     */
    void commit();

    /**
     * End the session, and mark for reaping
     */
    void expire();

    /**
     * Checks if this transaction is associated with a specific user.
     * If username is null, it is assumed that the transaction is
     * anonymous and thus not bound to any user.
     * @param userName the user
     * @return true if the userName is associated with this transaction
     */
    public boolean isAssociatedWithUser(final String userName);

    /**
     * Discard all unpersisted changes and expire
     */
    void rollback();

    /**
     * Roll forward the expiration date for recent activity
     */
    void updateExpiryDate();

}
