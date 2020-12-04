
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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.kernel.api.services.UpdatePropertiesService;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static org.fcrepo.kernel.api.RdfCollectors.toModel;

/**
 * This class implements the update properties operation.
 *
 * @author dbernstein
 */
@Component
public class UpdatePropertiesServiceImpl extends AbstractService implements UpdatePropertiesService {

    @Inject
    private ReplacePropertiesService replacePropertiesService;

    @Inject
    private PersistentStorageSessionManager persistentStorageSessionManager;

    @Override
    public void updateProperties(final Transaction tx, final String userPrincipal,
                                 final FedoraId fedoraId, final String sparqlUpdateStatement)
            throws MalformedRdfException, AccessDeniedException {
        final var txId = tx.getId();
        try {
            final var psession = persistentStorageSessionManager.getSession(txId);
            final var triples = psession.getTriples(fedoraId, null);
            final Model model = triples.collect(toModel());
            final UpdateRequest request = UpdateFactory.create(sparqlUpdateStatement, fedoraId.getFullId());
            UpdateAction.execute(request, model);
            replacePropertiesService.perform(tx, userPrincipal, fedoraId, model);
        } catch (final PersistentItemNotFoundException ex) {
            throw new ItemNotFoundException(ex.getMessage(), ex);
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(ex.getMessage(), ex);
        }

    }
}
