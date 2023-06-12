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

    private static final String LOG_MESSAGE =
        "Cannot update %s because it is being updated by another transaction (%s).";
    private static final String HTTP_MESSAGE = "Cannot update %s because it is being updated by another transaction";

    private final String resource;
    private final String existingTx;
    private final String conflictingTx;

    /**
     * Constructor
     *
     * @param resource the Fedora response
     * @param conflictingTx the transaction id attempting to lock the resource
     * @param existingTx the transaction id holding the resource
     */
    public ConcurrentUpdateException(final String resource, final String conflictingTx, final String existingTx) {
        super(String.format(LOG_MESSAGE, resource, existingTx));
        this.resource = resource;
        this.conflictingTx = conflictingTx;
        this.existingTx = existingTx;
    }

    public String getResponseMessage() {
        return String.format(HTTP_MESSAGE, resource);
    }

    public String getExistingTransactionId() {
        return existingTx;
    }

    public String getConflictingTransactionId() {
        return conflictingTx;
    }
}
