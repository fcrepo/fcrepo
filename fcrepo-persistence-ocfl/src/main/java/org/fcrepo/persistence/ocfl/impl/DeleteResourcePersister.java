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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.DELETE;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.relativizeSubpath;

import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delete Resource Persister
 * @author whikloj
 */
public class DeleteResourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(DeleteResourcePersister.class);

    /**
     * Constructor.
     */
    public DeleteResourcePersister() {
        super(ResourceOperation.class, DELETE);
    }

    @Override
    public void persist(final OCFLObjectSession session, final ResourceOperation operation, final FedoraOCFLMapping mapping)
            throws PersistentStorageException {
        log.debug("Deleting {} from {}", operation.getResourceId(), mapping.getOcflObjectId());
        if (mapping.getParentFedoraResourceId().equals(operation.getResourceId())) {
            // We are at the root of the object.
            session.deleteObject();
        } else {
            final String subpath = relativizeSubpath(mapping.getParentFedoraResourceId(), operation.getResourceId());
            session.delete(subpath);
        }
    }
}
