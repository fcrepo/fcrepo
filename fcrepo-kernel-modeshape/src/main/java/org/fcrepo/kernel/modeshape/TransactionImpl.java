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
package org.fcrepo.kernel.modeshape;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.api.Transaction.State.COMMITED;
import static org.fcrepo.kernel.api.Transaction.State.DIRTY;
import static org.fcrepo.kernel.api.Transaction.State.NEW;
import static org.fcrepo.kernel.api.Transaction.State.ROLLED_BACK;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * A Fedora Transaction wraps a JCR session with some expiration logic.
 * Whenever the transaction's session is requested, the expiration is extended
 *
 * @author bbpennel
 */
public class TransactionImpl implements Transaction {

    // the default timeout is 3 minutes
    public static final long DEFAULT_TIMEOUT = 3L * 60L * 1000L;

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo.transactions.timeout";

    private final Session session;

    private final String id;

    private final String userName;

    private final Date created;

    private final Calendar expires;

    private State state = NEW;

    /**
     * Create a transaction for the given Session
     * @param session the given session
     * @param userName the user name
     */

    public TransactionImpl(final Session session, final String userName) {
        super();
        this.session = session;
        this.created = new Date();
        this.id = randomUUID().toString();
        this.expires = Calendar.getInstance();
        this.updateExpiryDate();
        this.userName = userName;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#getSession()
     */
    @Override
    public Session getSession() {
        updateExpiryDate();
        return TxAwareSession.newInstance(session, id);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#getCreated()
     */
    @Override
    public Date getCreated() {
        return new Date(created.getTime());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#getState()
     */
    @Override
    public State getState() throws RepositoryException {
        if (this.session != null && this.session.hasPendingChanges()) {
            return DIRTY;
        }
        return state;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#getExpires()
     */
    @Override
    public Date getExpires() {
        return expires.getTime();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#commit(org.fcrepo.kernel.api.services.VersionService)
     */
    @Override
    public void commit() {

        try {
            this.session.save();
            this.state = COMMITED;
            this.expire();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#expire()
     */
    @Override
    public void expire() {
        this.session.logout();
        this.expires.setTimeInMillis(currentTimeMillis());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#isAssociatedWithUser()
     */
    @Override
    public boolean isAssociatedWithUser(final String userName) {
        boolean associatedWith = false;
        if (this.userName == null) {
            if (userName == null) {
                associatedWith = true;
            }
        } else {
            associatedWith = this.userName.equals(userName);
        }
        return associatedWith;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#rollback()
     */
    @Override
    public void rollback() {
        this.state = ROLLED_BACK;
        try {
            this.session.refresh(false);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        this.expire();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.Transaction#updateExpiryDate()
     */
    @Override
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
