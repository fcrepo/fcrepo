/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl.operations;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateVersionResourceOperationBuilder;
import org.fcrepo.kernel.api.operations.VersionResourceOperationFactory;
import org.springframework.stereotype.Component;

/**
 * Default impl of {@link VersionResourceOperationFactory}
 *
 * @author pwinckles
 */
@Component
public class VersionResourceOperationFactoryImpl implements VersionResourceOperationFactory {

    @Override
    public CreateVersionResourceOperationBuilder createBuilder(final Transaction transaction, final FedoraId rescId) {
        return new CreateVersionResourceOperationBuilderImpl(transaction, rescId);
    }

}
