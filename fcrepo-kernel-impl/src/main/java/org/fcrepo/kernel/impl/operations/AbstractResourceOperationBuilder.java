/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ResourceOperationBuilder;

/**
 * Base resource operation builder to share class fields and userPrincipal method.
 * @author whikloj
 */
abstract public class AbstractResourceOperationBuilder implements ResourceOperationBuilder {

    protected FedoraId rescId;

    protected String userPrincipal;

    protected Transaction transaction;

    /**
     * Constructor.
     *
     * @param transaction the transaction
     * @param rescId the resource identifier.
     */
    public AbstractResourceOperationBuilder(final Transaction transaction, final FedoraId rescId) {
        this.transaction = transaction;
        this.rescId = rescId;
    }

    @Override
    public ResourceOperationBuilder userPrincipal(final String userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

}
