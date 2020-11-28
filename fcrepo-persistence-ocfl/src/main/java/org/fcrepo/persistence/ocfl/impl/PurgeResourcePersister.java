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
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.PURGE;

/**
 * Purge Resource Persister
 * @author whikloj
 */
class PurgeResourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(PurgeResourcePersister.class);

    protected PurgeResourcePersister(final FedoraToOcflObjectIndex fedoraOcflIndex) {
        super(ResourceOperation.class, PURGE, fedoraOcflIndex);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var mapping = getMapping(session.getId(), operation.getResourceId());
        final var resourceId = operation.getResourceId();
        final var objectSession = session.findOrCreateSession(mapping.getOcflObjectId());
        log.debug("Deleting {} from {}", resourceId, mapping.getOcflObjectId());

        try {
            objectSession.deleteResource(resourceId.getResourceId());
        } catch (final RuntimeException e) {
            throw new PersistentStorageException(String.format("Purge resource %s failed", resourceId), e);
        }

        ocflIndex.removeMapping(session.getId(), resourceId.asResourceId());
    }

}
