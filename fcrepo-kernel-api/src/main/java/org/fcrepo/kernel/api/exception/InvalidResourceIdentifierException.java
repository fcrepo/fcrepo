/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * This exception is used for invalid resource identifiers, such as when a resource path has empty segments.
 * Note: This exception is *not* used for valid identifiers that point to non-existent resources.
 *
 * @author awoods
 * @since July 14, 2015
 */
public class InvalidResourceIdentifierException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param msg root cause
     */
    public InvalidResourceIdentifierException(final String msg) {
        super(msg);
    }

    /**
     * Constructor
     *
     * @param msg root cause
     * @param e root cause exception
     */
    public InvalidResourceIdentifierException(final String msg, final Throwable e) {
        super(msg,e);
    }
}
