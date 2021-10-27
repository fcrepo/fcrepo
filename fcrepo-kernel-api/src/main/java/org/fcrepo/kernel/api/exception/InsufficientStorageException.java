/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Exception thrown when the device has insufficient storage to complete the operation.
 *
 * @author Daniel Bernstein
 * @since Oct 7, 2016
 */
public class InsufficientStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Exception with message
     *
     * @param message the message
     * @param t the throwable
     */
    public InsufficientStorageException(final String message, final Throwable t) {
        super(message, t);
    }
}
