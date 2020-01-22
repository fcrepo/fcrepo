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

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;

import javax.inject.Inject;

import java.time.Instant;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.services.GetResourceService;
import org.fcrepo.kernel.impl.models.BinaryImpl;
import org.fcrepo.kernel.impl.models.ContainerImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

/**
 * Implementation to check for or get FedoraResources from storage.
 * @author whikloj
 * @since 6.0.0
 */
@Component
public class GetResourceServiceImpl implements GetResourceService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private ResourceFactory rsFactory;

    @Override
    public FedoraResource getResource(final Transaction transaction, final String fedoraId, final Instant version) {
        final PersistentStorageSession psSession;
        if (transaction == null) {
            psSession = psManager.getReadOnlySession();
        } else {
            psSession = psManager.getSession(transaction.getId());
        }
        try {
            final ResourceHeaders headers = psSession.getHeaders(fedoraId, version);
            final FedoraResource resource;
            if (headers.getInteractionModel().equals(NON_RDF_SOURCE.toString())) {
                resource = new BinaryImpl(fedoraId, transaction, psManager, rsFactory);
            } else {
                resource = new ContainerImpl(fedoraId, transaction, psManager, rsFactory);
            }
            // Commit session so it doesn't hang around.
            psSession.commit();
            return resource;
        } catch (PersistentItemNotFoundException e) {
            // Object doesn't exist.
            return null;
        } catch (PersistentStorageException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean doesResourceExist(final Transaction transaction, final String fedoraId, final Instant version) {
        // TODO: Check the index first.

        final PersistentStorageSession psSession;
        if (transaction == null) {
            psSession = psManager.getReadOnlySession();
        } else {
            psSession = psManager.getSession(transaction.getId());
        }
        try {
            psSession.getHeaders(fedoraId, version);
            // Commit session so it doesn't hang around.
            psSession.commit();
            return true;
        } catch (PersistentItemNotFoundException e) {
            // Object doesn't exist.
            return false;
        } catch (PersistentStorageException e) {
            // Other error, pass along.
            throw new RepositoryRuntimeException(e);
        }
    }

}
