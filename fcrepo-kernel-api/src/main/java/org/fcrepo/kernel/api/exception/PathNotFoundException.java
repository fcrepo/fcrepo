/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;


/**
 * Indicates the path was not found in the repository.
 *
 * @author dbernstein
 * @author whikloj
 */
public class PathNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor
     *
     * @param msg the message
     */
    public PathNotFoundException(final String msg) {
        super(msg);
    }

    /**
     * Constructor for wrapping exception.
     *
     * @param message the original message.
     * @param cause the root cause.
     */
    public PathNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
