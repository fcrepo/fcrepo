/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

/**
 * This exception indicates that a resource could not be modified because it is currently being modified by another
 * transaction.
 *
 * @author pwinckles
 */
public class ConcurrentUpdateException extends RepositoryRuntimeException {

    /**
     * Constructor
     *
     * @param msg message
     */
    public ConcurrentUpdateException(final String msg) {
        super(msg);
    }

}
