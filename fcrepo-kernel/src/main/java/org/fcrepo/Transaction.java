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

package org.fcrepo;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A Fedora Transaction wraps a JCR session with some expiration logic.
 * Whenever the transaction's session is requested, the expiration is extended
 */
public class Transaction {

    // the default timeout is 3 minutes
    public static final long DEFAULT_TIMEOUT = 3l * 60l * 1000l;

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

    private final Calendar expires;

    private State state = State.NEW;

    /**
     * Create a transaction for the given Session
     * @param session
     */
    public Transaction(Session session) {
        super();
        this.session = session;
        this.created = new Date();
        this.id = UUID.randomUUID().toString();
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
            return State.DIRTY;
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
     * "Commit" the transaction by saving the backing-session
     * @throws RepositoryException
     */
    public void commit() throws RepositoryException {
        this.session.save();
        this.state = State.COMMITED;
        this.expire();
    }

    /**
     * End the session, and mark for reaping
     * @throws RepositoryException
     */
    public void expire() throws RepositoryException {
        this.session.logout();
        this.expires.setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Discard all unpersisted changes and expire
     * @throws RepositoryException
     */
    public void rollback() throws RepositoryException {
        this.state = State.ROLLED_BACK;
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
        this.expires.setTimeInMillis(System.currentTimeMillis() + duration);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((expires == null) ? 0 : expires.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((session == null) ? 0 : session.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Transaction)) {
            return false;
        }
        Transaction other = (Transaction) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (session == null) {
            if (other.session != null) {
                return false;
            }
        } else if (!session.equals(other.session)) {
            return false;
        }
        if (created == null) {
            if (other.created != null) {
                return false;
            }
        } else if (!created.equals(other.created)) {
            return false;
        }
        if (expires == null) {
            if (other.expires != null) {
                return false;
            }
        } else if (!expires.equals(other.expires)) {
            return false;
        }
        if (state != other.state) {
            return false;
        }
        return true;
    }
}
