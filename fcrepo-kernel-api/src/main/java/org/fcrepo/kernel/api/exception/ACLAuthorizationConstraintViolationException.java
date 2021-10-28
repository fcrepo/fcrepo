/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 *
 * @author mohideen
 * @since 2018-09-12
 */
public class ACLAuthorizationConstraintViolationException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public ACLAuthorizationConstraintViolationException(final String msg) {
        super(msg);
    }

}
