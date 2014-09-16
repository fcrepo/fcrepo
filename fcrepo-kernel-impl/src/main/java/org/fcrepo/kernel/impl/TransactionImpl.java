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
package org.fcrepo.kernel.impl;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.Transaction.State.NEW;
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

import org.fcrepo.kernel.Transaction;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.services.VersionService;

/**
 * A Fedora Transaction wraps a JCR session with some expiration logic.
 * Whenever the transaction's session is requested, the expiration is extended
 *
 * @author bbpennel
 */
public class TransactionImpl implements Transaction {

    // the default timeout is 3 minutes
    public static final long DEFAULT_TIMEOUT = 3L * 60L * 1000L;

    public static final String TIMEOUT_SYSTEM_PROPERTY = "fcrepo4.tx.timeout";

    private final Session session;

    private final String id;

    private final String userName;

    private final Date created;

    private final Calendar expires;

    private State state = NEW;

    private Set<String> versionedPaths;

    /**
     * Create a transaction for the given Session
     * @param session
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
     * @see org.fcrepo.kernel.Transaction#getSession()
     */
    @Override
    public Session getSession() {
        updateExpiryDate();
        return TxAwareSession.newInstance(session, id);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#getCreated()
     */
    @Override
    public Date getCreated() {
        return new Date(created.getTime());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#getState()
     */
    @Override
    public State getState() throws RepositoryException {
        if (this.session != null && this.session.hasPendingChanges()) {
            return DIRTY;
        }
        return state;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#getExpires()
     */
    @Override
    public Date getExpires() {
        return expires.getTime();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#addPathToVersion(java.lang.String)
     */
    @Override
    public void addPathToVersion(final String absPath) {
        if (versionedPaths == null) {
            versionedPaths = Collections.newSetFromMap(
                    new ConcurrentHashMap<String, Boolean>());
        }
        versionedPaths.add(absPath);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#commit(org.fcrepo.kernel.services.VersionService)
     */
    @Override
    public void commit(final VersionService vService) {

        try {
            this.session.save();
            if (this.versionedPaths != null) {
                if (vService == null) {
                    throw new IllegalStateException("Versioned Paths were added," +
                            " but no VersionService was provided!");
                }
                vService.createVersion(session.getWorkspace(), versionedPaths);
            }
            this.state = COMMITED;
            this.expire();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#expire()
     */
    @Override
    public void expire() {
        this.session.logout();
        this.expires.setTimeInMillis(currentTimeMillis());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.Transaction#isAssociatedWithUser()
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
     * @see org.fcrepo.kernel.Transaction#rollback()
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
     * @see org.fcrepo.kernel.Transaction#updateExpiryDate()
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
