/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * An invalid memento path constraint has been violated.
 *
 * @author lsitu
 * @since 2018-08-10
 */
public class InvalidMementoPathException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public InvalidMementoPathException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public InvalidMementoPathException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

}
