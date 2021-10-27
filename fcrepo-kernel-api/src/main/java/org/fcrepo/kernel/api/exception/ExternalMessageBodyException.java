/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Indicates that an external body request has failed
 *
 * @author bseeger
 * @since April 18, 2018
 */
public class ExternalMessageBodyException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor
     *
     * @param msg the message
     */
    public ExternalMessageBodyException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public ExternalMessageBodyException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }
}
