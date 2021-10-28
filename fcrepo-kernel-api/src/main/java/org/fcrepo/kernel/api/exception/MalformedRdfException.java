/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * Indicates that RDF was presented for persistence to the repository, but could not be persisted for some reportable
 * reason.
 *
 * @author ajs6f
 * @author whikloj
 * @since Oct 24, 2013
 */
public class MalformedRdfException extends ConstraintViolationException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public MalformedRdfException(final String msg) {
        super(msg);
    }


    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public MalformedRdfException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

}
