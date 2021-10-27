/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Exception when attempting to access an external-content URI has problems.
 *
 * @author whikloj
 * @since 2018-07-11
 */
public class ExternalContentAccessException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param msg the message
     * @param rootCause the causing Exception
     */
    public ExternalContentAccessException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

}
