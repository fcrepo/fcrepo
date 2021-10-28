/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api.exception;

import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_NOT_MODIFIED;

/**
 * @author dbernstein
 * @since Jun 22, 2017
 */
public class PreconditionException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    private int httpStatus;

    /**
     * Ordinary constructor
     *
     * @param msg error message
     * @param httpStatus the http status code
     */
    public PreconditionException(final String msg, final int httpStatus) {
        super(msg);
        if (httpStatus != SC_PRECONDITION_FAILED && httpStatus != SC_NOT_MODIFIED) {
            throw new IllegalArgumentException("Invalid httpStatus (" + httpStatus +
                    "). The http status for PreconditionExceptions must be " +
                    SC_PRECONDITION_FAILED + " or " + SC_NOT_MODIFIED);
        }
        this.httpStatus = httpStatus;
    }

    /**
     * @return the httpStatus
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}
