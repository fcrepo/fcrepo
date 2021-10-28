/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;

/**
 * If an operation results in a conflict preventing the successful completion of a persistence
 * operation.
 *
 * @author dbernstein
 * @since 2020-01-29
 */
public class PersistentItemConflictException extends PersistentStorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor
     *
     * @param msg The text of the exception.
     */
    public PersistentItemConflictException(final String msg) {
        super(msg);
    }

}
