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

import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraToOCFLObjectIndex;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;

/**
 * This class implements the persistence of an existing RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
public class UpdateRDFSourcePersister extends AbstractRDFSourcePersister {

    private static final Logger log = LoggerFactory.getLogger(UpdateRDFSourcePersister.class);

    /**
     * Constructor
     * @param objectFactory The OCFL Object factory
     * @param index The FedoraToOCFLObjectIndex
     */
    public UpdateRDFSourcePersister(final OCFLObjectSessionFactory objectFactory,
                                    final FedoraToOCFLObjectIndex index) {
        super(RdfSourceOperation.class, UPDATE, objectFactory, index);
    }

    @Override
    public void persist(final OCFLPersistentStorageSession session, final ResourceOperation operation)
            throws PersistentStorageException {
        final FedoraOCFLMapping fedoraOCFLMapping = getMapping(operation.getResourceId());
        final OCFLObjectSession objSession = session.findOrCreateSession(fedoraOCFLMapping.getOcflObjectId());
        persistRDF(objSession, operation, fedoraOCFLMapping);
    }
}
