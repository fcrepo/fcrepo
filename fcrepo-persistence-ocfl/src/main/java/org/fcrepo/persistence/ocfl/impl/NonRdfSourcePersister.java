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
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.INTERNAL_FEDORA_DIRECTORY;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.relativizeSubpath;
import static org.fcrepo.persistence.ocfl.OCFLPersistentStorageUtils.writeRDF;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the persistence of a NonRDFSource
 *
 * @author whikloj
 * @since 6.0.0
 */
public class NonRdfSourcePersister extends AbstractPersister {

    private static final Logger log = LoggerFactory.getLogger(NonRdfSourcePersister.class);

    private static final Set<ResourceOperationType> OPERATION_ACTIONS = new HashSet<>(asList(CREATE, UPDATE));

    /**
     * Constructor
     */
    public NonRdfSourcePersister() {
        super(NonRdfSourceOperation.class, OPERATION_ACTIONS);
    }

    @Override
    public void persist(final OCFLObjectSession session, final ResourceOperation operation,
                        final FedoraOCFLMapping mapping) throws PersistentStorageException {
        log.debug("persisting ({}) to {}", operation.getResourceId(), mapping.getOcflObjectId());
        final String subpath = relativizeSubpath(mapping.getParentFedoraResourceId(), operation.getResourceId());

        // write user content
        final NonRdfSourceOperation nonRdfSourceOperation = ((NonRdfSourceOperation) operation);
        session.write(subpath, nonRdfSourceOperation.getContentStream());

        //write server props
        writeRDF(session, operation.getServerManagedProperties(), INTERNAL_FEDORA_DIRECTORY + File.separator + subpath);
    }
}
