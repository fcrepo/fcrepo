/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import com.google.common.annotations.VisibleForTesting;

import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.VersionResourceOperationFactory;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

/**
 * Implementation of {@link VersionService}
 *
 * @author dbernstein
 */
@Component
public class VersionServiceImpl extends AbstractService implements VersionService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private VersionResourceOperationFactory versionOperationFactory;

    @Override
    public void createVersion(final Transaction transaction, final FedoraId fedoraId, final String userPrincipal) {
        final var session = psManager.getSession(transaction);
        final var operation = versionOperationFactory.createBuilder(transaction, fedoraId)
                .userPrincipal(userPrincipal)
                .build();

        lockArchivalGroupResource(transaction, session, fedoraId);
        final var headers = session.getHeaders(fedoraId, null);
        if (RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI.equals(headers.getInteractionModel())) {
            transaction.lockResource(fedoraId.asBaseId());
        }
        if (RdfLexicon.NON_RDF_SOURCE.toString().equals(headers.getInteractionModel())) {
            transaction.lockResource(fedoraId.asDescription());
        }
        transaction.lockResource(fedoraId);

        try {
            session.persist(operation);
            recordEvent(transaction, fedoraId, operation);
        } catch (final PersistentStorageException e) {
            throw new RepositoryRuntimeException(String.format("Failed to create new version of %s",
                    fedoraId.getResourceId()), e);
        }
    }

    @VisibleForTesting
    void setPsManager(final PersistentStorageSessionManager psManager) {
        this.psManager = psManager;
    }

    @VisibleForTesting
    void setVersionOperationFactory(final VersionResourceOperationFactory versionOperationFactory) {
        this.versionOperationFactory = versionOperationFactory;
    }

}
