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
import org.fcrepo.persistence.ocfl.api.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.REINDEX;

/**
 * Reindex resource rersister
 *
 * @author dbernstein
 */
class ReindexResourcePersister implements Persister {

    private static final Logger log = LoggerFactory.getLogger(ReindexResourcePersister.class);

    private ReindexService reindexService;

    /**
     * Constructor
     *
     * @param reindexService the reindex service
     */
    protected ReindexResourcePersister(final ReindexService reindexService) {
        this.reindexService = reindexService;
    }

    @Override
    public boolean handle(final ResourceOperation operation) {
        return operation != null && REINDEX.equals(operation.getType());
    }

    @Override
    public void persist(final OcflPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final var ocflId = operation.getResourceId().getBaseId();
        try {
            this.reindexService.indexOcflObject(session.getId(), ocflId);
        } catch (Exception ex) {
            throw new PersistentStorageException(ex.getMessage(), ex);
        }
    }
}
