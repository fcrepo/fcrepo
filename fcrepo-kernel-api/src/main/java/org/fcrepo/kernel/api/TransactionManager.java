/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api;

/**
 * The Fedora Transaction Manager abstraction
 *
 * @author mohideen
 */
public interface TransactionManager {

    /**
     * Create a new fedora transaction
     *
     * @return {@link Transaction} The new fedora transaction
     */
    Transaction create();

    /**
     * Get an existing fedora transaction
     *
     * @param transactionId the id of the transaction to be returned
     * @return {@link Transaction} the fedora transaction associated with the provided id
     */
    Transaction get(String transactionId);
}
