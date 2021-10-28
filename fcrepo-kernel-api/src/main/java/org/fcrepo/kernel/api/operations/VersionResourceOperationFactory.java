/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.api.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * Factory for creating {@link CreateVersionResourceOperationBuilder}s
 *
 * @author pwinckles
 */
public interface VersionResourceOperationFactory extends ResourceOperationFactory {

    /**
     * Create a new {@link CreateVersionResourceOperationBuilder} builder.
     *
     * @param transaction the transaction
     * @param rescId the resource id
     * @return builder
     */
    CreateVersionResourceOperationBuilder createBuilder(Transaction transaction, FedoraId rescId);

}
