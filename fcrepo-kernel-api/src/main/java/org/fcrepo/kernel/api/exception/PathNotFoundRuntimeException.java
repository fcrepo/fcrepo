/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * @author cabeer
 * @since 9/15/14
 */
public class PathNotFoundRuntimeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Wrap a PathNotFoundException in a runtime exception
     * @param message the original message.
     * @param rootCause the root cause.
     */
    public PathNotFoundRuntimeException(final String message, final Throwable rootCause) {
        super(message, rootCause);
    }

    /**
     * Create a PathNotFoundException directly.
     * @param message the original message.
     */
    public PathNotFoundRuntimeException(final String message) {
        super(message);
    }
}
