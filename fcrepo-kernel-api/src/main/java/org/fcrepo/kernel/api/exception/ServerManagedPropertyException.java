/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * @author cabeer
 * @author whikloj
 * @since 10/1/14
 */
public class ServerManagedPropertyException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     *
     * @param msg the message
     */
    public ServerManagedPropertyException(final String msg) {
        super(msg);
    }
}
