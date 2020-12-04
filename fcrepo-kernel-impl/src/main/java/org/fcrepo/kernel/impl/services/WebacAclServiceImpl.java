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
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.WebacAcl;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.services.WebacAclService;
import org.fcrepo.kernel.impl.models.WebacAclImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_WEBAC_ACL_URI;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;

/**
 * Implementation of {@link WebacAclService}
 *
 * @author dbernstein
 */
@Component
public class WebacAclServiceImpl extends AbstractService implements WebacAclService {

    @Inject
    private PersistentStorageSessionManager psManager;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private RdfSourceOperationFactory rdfSourceOperationFactory;

    @Override
    public WebacAcl find(final Transaction transaction, final FedoraId fedoraId) {
        try {
            return resourceFactory.getResource(transaction, fedoraId, WebacAclImpl.class);
        } catch (final PathNotFoundException exc) {
            throw new PathNotFoundRuntimeException(exc.getMessage(), exc);
        }
    }

    @Override
    public void create(final Transaction transaction, final FedoraId fedoraId, final String userPrincipal,
                                 final Model model) {
        final PersistentStorageSession pSession = this.psManager.getSession(transaction.getId());

        ensureValidACLAuthorization(model);

        final RdfStream stream = fromModel(model.getResource(fedoraId.getFullId()).asNode(), model);

        final RdfSourceOperation createOp = rdfSourceOperationFactory
                .createBuilder(fedoraId, FEDORA_WEBAC_ACL_URI)
                .parentId(fedoraId.asBaseId())
                .triples(stream)
                .relaxedProperties(model)
                .userPrincipal(userPrincipal)
                .build();

        lockArchivalGroupResourceFromParent(transaction, pSession, fedoraId.asBaseId());
        transaction.lockResource(fedoraId);

        try {
            pSession.persist(createOp);
            recordEvent(transaction.getId(), fedoraId, createOp);
        } catch (final PersistentStorageException exc) {
            throw new RepositoryRuntimeException(String.format("failed to create resource %s", fedoraId), exc);
        }
    }

}
