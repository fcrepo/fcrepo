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

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.DELETE;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createFilesystemRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import static java.lang.System.currentTimeMillis;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.UUID;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.kernel.impl.operations.DeleteResourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchResult;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.cache.NoOpCache;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

/**
 * Abstract Reindexer test with common setup and functions.
 * @author dbernstein
 * @author whikloj
 * @since 6.0.0
 */
public class AbstractReindexerTest {

    protected PersistentStorageSessionManager persistentStorageSessionManager;

    protected FedoraToOcflObjectIndex ocflIndex;

    protected MutableOcflRepository repository;

    protected OcflObjectSessionFactory ocflObjectSessionFactory;

    @Mock
    protected ContainmentIndex containmentIndex;

    @Mock
    protected SearchIndex searchIndex;

    @Mock
    protected ReferenceService referenceService;

    @Mock
    protected MembershipService membershipService;

    @Mock
    protected SearchResult containerResult;

    @Mock
    protected OcflPropsConfig propsConfig;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected final String session1Id = "session1";
    protected final FedoraId resource1 = FedoraId.create("resource1");
    protected final FedoraId resource2 =  resource1.resolve("resource2");

    public void setup() throws Exception {
        final var targetDir = Paths.get("target");
        final var dataDir = targetDir.resolve("test-fcrepo-data-" + currentTimeMillis());
        final var repoDir = dataDir.resolve("ocfl-repo");
        final var workDir = dataDir.resolve("ocfl-work");

        repository = createFilesystemRepository(repoDir, workDir);

        ocflIndex = new TestOcflObjectIndex();
        ocflIndex.reset();

        final var objectMapper = OcflPersistentStorageUtils.objectMapper();
        ocflObjectSessionFactory = new DefaultOcflObjectSessionFactory(repository,
                tempFolder.newFolder().toPath(), objectMapper, new NoOpCache<>(), CommitType.NEW_VERSION,
                "Fedora 6 test", "fedoraAdmin", "info:fedora/fedoraAdmin");

        persistentStorageSessionManager = new OcflPersistentSessionManager();
        setField(persistentStorageSessionManager, "ocflIndex", ocflIndex);
        setField(persistentStorageSessionManager, "objectSessionFactory", ocflObjectSessionFactory);

        when(propsConfig.getReindexBatchSize()).thenReturn(5L);
        when(propsConfig.getReindexingThreads()).thenReturn(1L);
        when(propsConfig.isReindexFailOnError()).thenReturn(true);
    }

    protected void assertDoesNotHaveOcflId(final FedoraId resourceId) {
        try {
            ocflIndex.getMapping(null, resourceId);
            fail(resourceId + " should not exist in index");
        } catch (final FedoraOcflMappingNotFoundException e) {
            //do nothing - expected
        }
    }

    protected void assertHasOcflId(final String expectedOcflId, final FedoraId resourceId)
            throws FedoraOcflMappingNotFoundException {
        assertEquals(FedoraTypes.FEDORA_ID_PREFIX + "/" + expectedOcflId,
                ocflIndex.getMapping(null, resourceId).getOcflObjectId());
    }

    protected void createResource(final PersistentStorageSession session,
                                final FedoraId resourceId, final boolean isArchivalGroup)
            throws PersistentStorageException {
        final var operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(resourceId);
        when(((CreateResourceOperation) operation).getParentId()).thenReturn(FedoraId.getRepositoryRootId());
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation)operation).isArchivalGroup()).thenReturn(isArchivalGroup);
        if (isArchivalGroup) {
            when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        } else {
            when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        }
        session.persist(operation);
    }

    protected void createChildResourceNonRdf(final PersistentStorageSession session,
                                           final FedoraId parentId, final FedoraId childId)
            throws PersistentStorageException {
        final var operation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(childId);
        when(operation.getType()).thenReturn(CREATE);
        final var bytes = "test".getBytes();
        final var stream = new ByteArrayInputStream(bytes);
        when(operation.getContentSize()).thenReturn((long)bytes.length);
        when(operation.getContentStream()).thenReturn(stream);
        when(operation.getMimeType()).thenReturn("text/plain");
        when(operation.getFilename()).thenReturn("test");
        when(((CreateResourceOperation)operation).getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        when(((CreateResourceOperation)operation).getParentId()).thenReturn(parentId);
        session.persist(operation);
    }

    protected void createChildResourceRdf(final PersistentStorageSession session, final FedoraId parentId,
                                        final FedoraId childId) {
        final var model = ModelFactory.createDefaultModel();
        model.add(
                ResourceFactory.createResource(childId.getFullId()),
                ResourceFactory.createProperty("http://purl.org/dc/elements/1.1/title"),
                ResourceFactory.createPlainLiteral("Title")
        );
        final var rdfStream = fromModel(createURI(childId.getFullId()), model);
        final var operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(childId);
        when(((CreateResourceOperation)operation).getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
        when(((CreateResourceOperation) operation).getParentId()).thenReturn(parentId);
        when(operation.getType()).thenReturn(CREATE);
        when(operation.getTriples()).thenReturn(rdfStream);
        session.persist(operation);
    }

    protected void deleteResource(final PersistentStorageSession session, final FedoraId resourceId)
            throws PersistentStorageException {
        final var operation = mock(DeleteResourceOperation.class);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(operation.getType()).thenReturn(DELETE);
        session.persist(operation);
    }

    protected String getRandomId() {
        return UUID.randomUUID().toString();
    }
}
