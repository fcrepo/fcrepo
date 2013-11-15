/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.Transaction.State.COMMITED;
import static org.fcrepo.kernel.Transaction.State.DIRTY;
import static org.fcrepo.kernel.Transaction.State.ROLLED_BACK;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A Fedora Transaction wraps a JCR session with some expiration logic.
 * Whenever the transaction's session is requested, the expiration is extended
 */
public class Transaction {

    // the default timeout is 3 minutes
    public static final long DEFAULT_TIMEOUT = 3L * 60L * 1000L;

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo4.tx.timeout";

    /**
     * Information about the state of the transaction
     */
    public static enum State {
        DIRTY, NEW, COMMITED, ROLLED_BACK
    }

    private final Session session;

    private final String id;

    private final Date created;

    private Calendar expires;

    private State state = State.NEW;

    private Set<String> versionedPaths = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Create a transaction for the given Session
     * @param session
     */
    public Transaction(final Session session) {
        super();
        this.session = session;
        this.created = new Date();
        this.id = randomUUID().toString();
        this.expires = Calendar.getInstance();
        this.updateExpiryDate();
    }

    /**
     * Get the transaction-aware session
     * @return
     */
    public Session getSession() {
        updateExpiryDate();
        return TxAwareSession.newInstance(session, id);
    }

    /**
     * Get the date this transaction was created
     * @return
     */
    public Date getCreated() {
        return created;
    }

    /**
     * Get the transaction identifier
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Get the state of this transaction
     * @return
     * @throws RepositoryException
     */
    public State getState() throws RepositoryException {
        if (this.session != null && this.session.hasPendingChanges()) {
            return DIRTY;
        }
        return state;
    }

    /**
     * Get the Date when this transaction is expired and can be
     * garbage-collected
     * @return
     */
    public Date getExpires() {
        return expires.getTime();
    }

    /**
     * Adds a path at which a new version should be made upon successful
     * completion of this transaction.  Subsequent calls with the same path
     * have no effect, as the entire transaction is meant to be atomic and only
     * one new version can result from it.
     * @param absPath the object path to the resource to have a version
     *                checkpoint made
     */
    public void addPathToVersion(String absPath) {
        versionedPaths.add(absPath);
    }

    /**
     * "Commit" the transaction by saving the backing-session
     * @throws RepositoryException
     */
    public void commit() throws RepositoryException {
        this.session.save();
        for (String path : versionedPaths) {
            session.getWorkspace().getVersionManager().checkpoint(path);
        }
        this.state = COMMITED;
        this.expire();
    }

    /**
     * End the session, and mark for reaping
     * @throws RepositoryException
     */
    public void expire() throws RepositoryException {
        this.session.logout();
        this.expires.setTimeInMillis(currentTimeMillis());
    }

    /**
     * Discard all unpersisted changes and expire
     * @throws RepositoryException
     */
    public void rollback() throws RepositoryException {
        this.state = ROLLED_BACK;
        this.session.refresh(false);
        this.expire();
    }

    /**
     * Roll forward the expiration date for recent activity
     */
    public void updateExpiryDate() {
        long duration;
        if (System.getProperty(TIMEOUT_SYSTEM_PROPERTY) != null) {
            duration =
                    Long.parseLong(System.getProperty(TIMEOUT_SYSTEM_PROPERTY));
        } else {
            duration = DEFAULT_TIMEOUT;
        }
        this.expires.setTimeInMillis(currentTimeMillis() + duration);
    }
}
