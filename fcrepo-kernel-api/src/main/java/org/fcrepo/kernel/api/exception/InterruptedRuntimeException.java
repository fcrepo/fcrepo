/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;


/**
 * Indicates an interruption to the current thread
 *
 * @since 2016-10-16
 * @author awoods
 */
public class InterruptedRuntimeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param msg message
     * @param e cause
     */
    public InterruptedRuntimeException(final String msg, final Throwable e) {
        super(msg, e);
    }

}
