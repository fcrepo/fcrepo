/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * A constraint has been violated.
 *
 * @author whikloj
 * @since 2015-05-29
 */
public class ConstraintViolationException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public ConstraintViolationException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public ConstraintViolationException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

}
