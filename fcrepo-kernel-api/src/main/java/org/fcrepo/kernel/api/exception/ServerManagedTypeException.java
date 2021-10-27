/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Certain mixin types are managed by the repository only.
 *
 * @author whikloj
 * @since 2015-06-02
 */
public class ServerManagedTypeException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * @param msg the message
     */
    public ServerManagedTypeException(final String msg) {
        super(msg);
    }

}
