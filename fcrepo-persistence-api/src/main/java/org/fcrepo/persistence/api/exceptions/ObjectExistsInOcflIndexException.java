/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * If an OCFL object already exists in the index.
 * @author whikloj
 * @since 6.4.1
 */
public class ObjectExistsInOcflIndexException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Basic constructor
     *
     * @param msg The text of the exception.
     */
    public ObjectExistsInOcflIndexException(final String msg) {
        super(msg);
    }

    /**
     * Constructor
     *
     * @param msg message
     * @param rootCause cause
     */
    public ObjectExistsInOcflIndexException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }
}
