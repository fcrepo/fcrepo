/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateVersionResourceOperationBuilder;
import org.fcrepo.kernel.api.operations.ResourceOperation;

/**
 * Default impl of {@link CreateVersionResourceOperationBuilder}
 *
 * @author pwinckles
 */
public class CreateVersionResourceOperationBuilderImpl extends AbstractResourceOperationBuilder
        implements CreateVersionResourceOperationBuilder {

    /**
     * Create a new builder
     *
     * @param transaction the transaction.
     * @param resourceId the resource id
     */
    public CreateVersionResourceOperationBuilderImpl(final Transaction transaction, final FedoraId resourceId) {
        super(transaction, resourceId);
    }

    @Override
    public ResourceOperation build() {
        final var operation = new CreateVersionResourceOperationImpl(transaction, rescId);
        operation.setUserPrincipal(userPrincipal);
        return operation;
    }

}
