/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.ReindexResourceOperationFactory;
import org.fcrepo.kernel.api.services.ReindexService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

/**
 * Implementation of {@link org.fcrepo.kernel.api.services.ReindexService}
 *
 * @author dbernstein
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ReindexServiceImpl extends AbstractService implements ReindexService {

    @Inject
    private TransactionManager transactionManager;

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Inject
    private ReindexResourceOperationFactory resourceOperationFactory;

    @Override
    public void reindexByFedoraId(final Transaction transaction, final String principal, final FedoraId fedoraId) {
        final var tx = transactionManager.get(transaction.getId());
        final var psession = persistentStorageSessionManager.getSession(transaction);
        final var operation = resourceOperationFactory.create(transaction, fedoraId)
                .userPrincipal(principal).build();
        tx.lockResource(fedoraId);

        psession.persist(operation);
    }
}
