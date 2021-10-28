/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;

/**
 * If an item is not found in the storage.
 *
 * @author whikloj
 * @since 2019-09-24
 */
public class PersistentItemNotFoundException extends PersistentStorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor
     *
     * @param msg The text of the exception.
     */
    public PersistentItemNotFoundException(final String msg) {
        super(msg);
    }

    /**
     * Constructor
     *
     * @param msg message
     * @param e cause
     */
    public PersistentItemNotFoundException(final String msg, final Throwable e) {
        super(msg, e);
    }

}
