/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Runtime exception
 *
 * @author bbpennel
 */
public class RepositoryRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public RepositoryRuntimeException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public RepositoryRuntimeException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }
}
