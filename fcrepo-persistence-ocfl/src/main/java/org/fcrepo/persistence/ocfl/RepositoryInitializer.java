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
package org.fcrepo.persistence.ocfl;


import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.persistence.ocfl.impl.OcflPersistentSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_ROOT;

/**
 * This class is responsible for initializing the repository on start-up.
 *
 * @author dbernstein
 */
@Component
public class RepositoryInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryInitializer.class);

    @Inject
    private OcflPersistentSessionManager sessionManager;

    @Inject
    private RdfSourceOperationFactory operationFactory;

    @Inject
    private IndexBuilder indexBuilder;

    /**
     * Initializes the repository
     */
    @PostConstruct
    public void initialize() {
        //check that the root is initialized
        final PersistentStorageSession session = this.sessionManager.getSession("initializationSession" +
                                                                                 System.currentTimeMillis());

        indexBuilder.rebuildIfNecessary();

        final var root = FedoraId.getRepositoryRootId();

        try {
            try {
                session.getHeaders(root, null);
            } catch (PersistentItemNotFoundException e) {
                LOGGER.info("Repository root ({}) not found. Creating...", root);
                final Stream<Triple> repositoryRootTriples = Stream.of(
                        new Triple(createURI(root.getFullId()), RDF.type.asNode(), REPOSITORY_ROOT.asNode())
                );

                final RdfStream repositoryRootStream = new DefaultRdfStream(createURI(root.getFullId()),
                        repositoryRootTriples);
                final RdfSourceOperation operation = this.operationFactory.createBuilder(root,
                        BASIC_CONTAINER.getURI())
                        .parentId(root)
                        .triples(repositoryRootStream).build();

                session.persist(operation);
                session.commit();
                LOGGER.info("Successfully create repository root ({}).", root);
            }

        } catch (PersistentStorageException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

}
