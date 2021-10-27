/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Exception thrown when the calculated digest does not match the stored digest
 * @author ajs6f
 * @since Mar 10, 2013
 */
public class InvalidChecksumException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Exception with message
     * @param message the message
     */
    public InvalidChecksumException(final String message) {
        super(message);
    }
}
