/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * An access exception has occurred.
 *
 * @author acoburn
 * @since 2016-02-04
 */
public class AccessDeniedException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param msg message
     * @param e cause
     */
    public AccessDeniedException(final String msg, final Throwable e) {
        super(msg, e);
    }

}
