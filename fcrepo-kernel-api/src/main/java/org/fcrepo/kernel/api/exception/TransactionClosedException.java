/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Exception indicating that the requested transaction has been closed
 *
 * @author bbpennel
 */
public class TransactionClosedException extends TransactionRuntimeException {
    private static final long serialVersionUID = 1L;
    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public TransactionClosedException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public TransactionClosedException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }
}
