/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;


/**
 * Indicates a namespace used in a CRUD request has not been registered in the repository
 *
 * @author whikloj
 * @since September 12, 2014
 */
public class FedoraInvalidNamespaceException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor
     *
     * @param msg the message
     */
    public FedoraInvalidNamespaceException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public FedoraInvalidNamespaceException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

}
