/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateVersionResourceOperation;

/**
 * Default impl of {@link CreateVersionResourceOperation}
 *
 * @author pwinckles
 */
public class CreateVersionResourceOperationImpl extends AbstractResourceOperation
        implements CreateVersionResourceOperation {

    protected CreateVersionResourceOperationImpl(final Transaction transaction, final FedoraId rescId) {
        super(transaction, rescId);
    }

}
