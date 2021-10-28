/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * A transaction was not found in the transaction registry
 *
 * @author awoods
 */
public class SessionMissingException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 2139084821001303830L;

    /**
     *
     * @param s the exception message
     */
    public SessionMissingException(final String s) {
        super(s);
    }
}
