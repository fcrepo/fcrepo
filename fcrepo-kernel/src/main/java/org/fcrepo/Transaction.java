/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class Transaction {

    // the default timeout is 3 minutes
    public static final long DEFAULT_TIMEOUT =
        3l * 60l * 1000l;

    public static final String TIMEOUT_SYSTEM_PROPERTY =
        "fcrepo4.tx.timeout";

    /**
     * @todo Add Documentation.
     */
    public static enum State {
        DIRTY, NEW, COMMITED, ROLLED_BACK;
    }

    private final Session session;

    private final String id;

    private final Date created;

    private Calendar expires;

    private State state = State.NEW;

    private Transaction() {
        this.session = null;
        this.created = null;
        this.id = null;
        this.expires = null;
    }

    /**
     * @todo Add Documentation.
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
     * @todo Add Documentation.
     */
    public Session getSession() {
        updateExpiryDate();
        return TxAwareSession.newInstance(session, id);
    }

    public Session getJcrSession() {
        return session;
    }

    /**
     * @todo Add Documentation.
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @todo Add Documentation.
     */
    public String getId() {
        return id;
    }

    /**
     * @todo Add Documentation.
     */
    public State getState() throws RepositoryException {
        if (this.session != null && this.session.hasPendingChanges()) {
            return State.DIRTY;
        }
        return state;
    }

    /**
     * @todo Add Documentation.
     */
    public Date getExpires() {
        return expires.getTime();
    }

    /**
     * @todo Add Documentation.
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
            duration = Long.parseLong(System.getProperty(TIMEOUT_SYSTEM_PROPERTY));
        }else{
            duration = DEFAULT_TIMEOUT;
        }
        this.expires.setTimeInMillis(System.currentTimeMillis() + duration);
    }
}
