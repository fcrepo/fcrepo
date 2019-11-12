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

import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.UPDATE;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.getInternalFedoraDirectory;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.relativizeSubpath;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.writeRDF;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the persistence of a new RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
public class RDFSourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(RDFSourcePersister.class);

    private static final Set<ResourceOperationType> OPERATION_TYPES = new HashSet<>(asList(CREATE, UPDATE));

    /**
     * Constructor
     */
    public RDFSourcePersister() {
        super(RdfSourceOperation.class, OPERATION_TYPES);
    }

    @Override
    public void persist(final OCFLObjectSession session, final ResourceOperation operation,
                        final FedoraOCFLMapping mapping) throws PersistentStorageException {
        log.debug("persisting RDFSource ({}) to {}", operation.getResourceId(), mapping.getOcflObjectId());
        final String subpath = relativizeSubpath(mapping.getParentFedoraResourceId(), operation.getResourceId());

        //write user triples
        writeRDF(session, ((RdfSourceOperation) operation).getTriples(), subpath);

        //write server props
        writeRDF(session, operation.getServerManagedProperties(), getInternalFedoraDirectory() + subpath);
    }
}
