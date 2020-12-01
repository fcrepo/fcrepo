/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import javax.inject.Inject;

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
        final var session = psManager.getSession(transaction.getId());
        final var operation = versionOperationFactory.createBuilder(fedoraId)
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
            recordEvent(transaction.getId(), fedoraId, operation);
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
