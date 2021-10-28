/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Operation for manipulating a resource
 *
 * @author bbpennel
 */
public interface ResourceOperation {

    /**
     * The transaction the operation is in.
     *
     * @return the transaction.
     */
    Transaction getTransaction();

    /**
     * Id of the resource
     *
     * @return the ID.
     */
    FedoraId getResourceId();

    /**
     * Returns the user principal performing this operation
     *
     * @return the user principal performing this operation
     */
    String getUserPrincipal();

    /**
     * Returns the type of operation represented by this request
     *
     * @return operation type
     */
    ResourceOperationType getType();
}
