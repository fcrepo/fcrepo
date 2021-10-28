/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperation;

/**
 * Abstract operation for interacting with a resource
 *
 * @author bbpennel
 */
public abstract class AbstractResourceOperation implements ResourceOperation {

    /**
     * The internal Fedora ID.
     */
    private final FedoraId rescId;

    private String userPrincipal;

    private Transaction transaction;

    protected AbstractResourceOperation(final Transaction transaction, final FedoraId rescId) {
        this.rescId = rescId;
        this.transaction = transaction;
    }

    @Override
    public FedoraId getResourceId() {
        return rescId;
    }

    @Override
    public String getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    /**
     * @param userPrincipal the userPrincipal to set
     */
    public void setUserPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

}
