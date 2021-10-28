/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Thrown in circumstances where a client has used an unknown or unsupported hash algorithm
 * in a request, e.g. with `Digest` or `Want-Digest`.
 *
 * @author harring
 * @since 2017-09-12
 */
public class UnsupportedAlgorithmException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Exception with message
     * @param message the message
     */
    public UnsupportedAlgorithmException(final String message) {
        super(message);
    }

    /**
     * Ordinary constructor.
     *
     * @param message the message
     * @param rootCause the root cause
     */
    public UnsupportedAlgorithmException(final String message, final Throwable rootCause) {
        super(message, rootCause);
    }
}
