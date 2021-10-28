/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * @author cabeer
 * @since 9/15/14
 */
public class RepositoryVersionRuntimeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Wrap a RepositoryVersionException in a runtime exception
     * @param msg the message
     */
    public RepositoryVersionRuntimeException(final String msg) {
        super(msg);
    }
}
