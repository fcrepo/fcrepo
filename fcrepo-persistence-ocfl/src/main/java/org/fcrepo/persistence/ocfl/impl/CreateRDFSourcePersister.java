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

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.persistence.ocfl.api.OCFLObjectSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static java.lang.String.format;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;

/**
 * This class implements the persistence of a new RDFSource
 *
 * @author dbernstein
 * @since 6.0.0
 */
public class CreateRDFSourcePersister extends AbstractPersister<RdfSourceOperation> {

    private static final Logger log = LoggerFactory.getLogger(CreateRDFSourcePersister.class);

    /**
     * Constructor
     */
    public CreateRDFSourcePersister() {
        super(CREATE);
    }

    @Override
    public void persist(final OCFLObjectSession session, final RdfSourceOperation operation,
                        final FedoraOCFLMapping mapping) {
        log.debug("creating RDFSource ({}) to {}", operation.getResourceId(), mapping.getOcflObjectId());
        final String subpath = getSubpath(mapping.getParentFedoraResourceId(), operation.getResourceId());

        //write user triples
        write(session, operation.getTriples(), subpath + ".n3", mapping);

        //write server props
        write(session, operation.getServerManagedProperties(), ".fcrepo/" + subpath + ".n3", mapping);
    }

    private void write(final OCFLObjectSession session, final RdfStream triples, final String subpath, final FedoraOCFLMapping mapping) {
        try (final PipedOutputStream os = new PipedOutputStream()) {
            final PipedInputStream is = new PipedInputStream();
            os.connect(is);
            writeTriples(triples, NTRIPLES, os);
            session.write(subpath, is);
            log.debug("wrote {} to {}", subpath, mapping.getOcflObjectId());
        } catch (final IOException ex) {
            throw new RuntimeException(format("failed to write subpath %s within %s", subpath, mapping.getOcflObjectId()));
        }
    }
}