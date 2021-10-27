/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * Generic exception for things PersistentStorage related.
 *
 * @author whikloj
 * @since 2019-09-20
 */
public class PersistentStorageException extends RepositoryRuntimeException {

    /**
     * version UID.
     */
    private static final long serialVersionUID = -1L;

    /**
     * Constructor.
     *
     * @param msg the message
     */
    public PersistentStorageException(final String msg) {
        super(msg);
    }

    /**
     * Constructor
     *
     * @param msg message
     * @param e cause
     */
    public PersistentStorageException(final String msg, final Throwable e) {
        super(msg, e);
    }

}
