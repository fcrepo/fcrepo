/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;


/**
 * Exception indicating that a persistence session is closed.
 *
 * @author bbpennel
 */
public class PersistentSessionClosedException extends PersistentStorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor with message
     *
     * @param msg message
     */
    public PersistentSessionClosedException(final String msg) {
        super(msg);
    }

    /**
     * Constructor with message and cause
     *
     * @param msg message
     * @param e cause
     */
    public PersistentSessionClosedException(final String msg, final Throwable e) {
        super(msg, e);
    }
}
