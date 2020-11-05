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

import org.fcrepo.kernel.api.operations.CreateVersionResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.common.ResourceHeaderUtils;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.CommitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persister for creating a new OCFL version of a resource. The new version is not created until the session
 * is committed.
 *
 * @author pwinckles
 */
public class CreateVersionPersister extends AbstractPersister {

    private static final Logger LOG = LoggerFactory.getLogger(CreateVersionPersister.class);

    protected CreateVersionPersister(final FedoraToOcflObjectIndex index) {
        super(CreateVersionResourceOperation.class, ResourceOperationType.UPDATE, index);
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {

        final var resourceId = operation.getResourceId();
        LOG.debug("creating new version of <{}> in session <{}>", resourceId, session);

        final var archivalGroupId = findArchivalGroupInAncestry(resourceId, session);

        if (archivalGroupId.isPresent() && !archivalGroupId.get().equals(resourceId)) {
            throw new PersistentItemConflictException(
                    String.format("Resource <%s> is contained in Archival Group <%s> and cannot be versioned directly."
                            + " Version the Archival Group instead.", resourceId, archivalGroupId));
        }

        final var ocflMapping = getMapping(session.getId(), resourceId);
        final var ocflObjectSession = session.findOrCreateSession(ocflMapping.getOcflObjectId());

        // Touching the last modified date is necessary so that resource that do not have any outstanding changes are
        // still versioned
        final var headers = new ResourceHeadersAdapter(ocflObjectSession.readHeaders(resourceId.getResourceId()))
                .asKernelHeaders();
        ResourceHeaderUtils.touchModificationHeaders(headers, operation.getUserPrincipal());

        ocflObjectSession.writeHeaders(new ResourceHeadersAdapter(headers).asStorageHeaders());
        // The version is not actually created until the session is committed
        ocflObjectSession.commitType(CommitType.NEW_VERSION);
    }

}
