/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Request for object creation failed
 *
 * @author bseeger
 * @since 2017-04-07
 */
public class CannotCreateResourceException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public CannotCreateResourceException(final String msg) {
        super(msg);
    }

}
