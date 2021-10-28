/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * An exception class for invalid memento creation attempts.
 *
 * @author dbernstein
 */
public class CannotCreateMementoException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     *
     * @param msg the message
     */
    public CannotCreateMementoException(final String msg) {
        super(msg);
    }

}
