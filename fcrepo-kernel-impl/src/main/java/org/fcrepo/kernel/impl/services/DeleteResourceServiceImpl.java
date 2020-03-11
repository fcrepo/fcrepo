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

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.DeleteResourceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * This class mediates delete operations between the kernel and persistent storage layers
 *
 * @author dbernstein
 */
@Component
public class DeleteResourceServiceImpl extends AbstractService implements DeleteResourceService {

    private final static Logger log = LoggerFactory.getLogger(DeleteResourceService.class);

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private DeleteResourceOperationFactory deleteResourceFactory;

    @Inject
    private PersistentStorageSessionManager psManager;

    @Override
    public void perform(final Transaction tx, final FedoraResource fedoraResource) {
        final String fedoraResourceId = fedoraResource.getId();

        if (fedoraResource instanceof NonRdfSourceDescription) {
            throw new RepositoryRuntimeException(
                    format("A NonRdfSourceDescription cannot be deleted independently of the NonRDFSource:  %s",
                            fedoraResourceId));
        }


        try {
            log.debug("deleting of {}", fedoraResourceId);
            final PersistentStorageSession pSession = this.psManager.getSession(tx.getId());
            deleteDepthFirst(tx, pSession, fedoraResource);
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(format("failed to delete resource %s", fedoraResourceId), ex);
        }
    }


    private void deleteDepthFirst(final Transaction tx, final PersistentStorageSession pSession,
                                  final FedoraResource fedoraResource) throws PersistentStorageException {

        final String fedoraId = fedoraResource.getId();

        if (fedoraResource instanceof Container) {
            final Stream<String> children = containmentIndex.getContains(tx, fedoraResource);
            children.forEach(childResourceId -> {
                try {
                    final FedoraResource res = resourceFactory.getResource(tx, childResourceId);
                    deleteDepthFirst(tx, pSession, res);
                } catch (final PathNotFoundException ex) {
                    log.error("Path not found for {}: {}", fedoraId, ex.getMessage());
                    throw new PathNotFoundRuntimeException(ex);
                } catch (final PersistentStorageException ex) {
                    throw new RepositoryRuntimeException(format("failed to delete resource %s", fedoraId), ex);
                }
            });
        } else if (fedoraResource instanceof Binary) {
            //delete described resource if binary
            delete(tx, pSession, fedoraResource.getDescribedResource().getId());
        }

        //delete the acl if this is not the acl
        if (!fedoraResource.isAcl()) {
            final FedoraResource acl = fedoraResource.getAcl();
            if (acl != null) {
                delete(tx, pSession, acl.getId());
            }
        }

        //delete the resource itself
        delete(tx, pSession, fedoraId);
    }

    private void delete(final Transaction tx, final PersistentStorageSession pSession, final String fedoraId)
            throws PersistentStorageException {
        log.debug("starting delete of {}", fedoraId);
        final ResourceOperation deleteOp = deleteResourceFactory.deleteBuilder(fedoraId).build();
        pSession.persist(deleteOp);
        containmentIndex.removeResource(tx.getId(), fedoraId);
        log.debug("deleted {}", fedoraId);
    }
}
