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

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createFilesystemRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import static java.lang.System.currentTimeMillis;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.cache.NoOpCache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Reindex manager tests.
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReindexManagerTest {

    private PersistentStorageSessionManager sessionManager;

    private FedoraToOcflObjectIndex index;

    @Mock
    private ReindexService reindexService;

    private ReindexManager reindexManager;

    private MutableOcflRepository repository;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final String session1Id = "session1";
    private final FedoraId resource1 = FedoraId.create("resource1");
    private final FedoraId resource2 =  resource1.resolve("resource2");

    @Before
    public void setup() throws Exception {
        final var targetDir = Paths.get("target");
        final var dataDir = targetDir.resolve("test-fcrepo-data-" + currentTimeMillis());
        final var repoDir = dataDir.resolve("ocfl-repo");
        final var workDir = dataDir.resolve("ocfl-work");

        repository = createFilesystemRepository(repoDir, workDir);

        index = new TestOcflObjectIndex();
        index.reset();

        final var objectMapper = OcflPersistentStorageUtils.objectMapper();
        final var ocflObjectSessionFactory = new DefaultOcflObjectSessionFactory(repository,
                tempFolder.newFolder().toPath(),
                objectMapper,
                new NoOpCache<>(),
                CommitType.NEW_VERSION,
                "Fedora 6 test", "fedoraAdmin", "info:fedora/fedoraAdmin");

        sessionManager = new OcflPersistentSessionManager();
        setField(sessionManager, "ocflIndex", index);
        setField(sessionManager, "objectSessionFactory", ocflObjectSessionFactory);
    }

    @Test
    public void testProcessAnObject() throws Exception {
        final var session = sessionManager.getSession(session1Id);

        createResource(session, resource1, true);
        createChildResource(session, resource1, resource2);

        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        index.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        mockedRebuild();

        verify(reindexService).indexOcflObject(anyString(), eq(FEDORA_ID_PREFIX + "/resource1"));
    }

    private void mockedRebuild() throws Exception {
        reindexManager = new ReindexManager(repository.listObjectIds(), reindexService, true, 1);
        reindexManager.start();
        reindexManager.commit();
    }

    private void assertDoesNotHaveOcflId(final FedoraId resourceId) {
        try {
            index.getMapping(null, resourceId);
            fail(resourceId + " should not exist in index");
        } catch (final FedoraOcflMappingNotFoundException e) {
            //do nothing - expected
        }
    }

    private void assertHasOcflId(final String expectedOcflId, final FedoraId resourceId)
            throws FedoraOcflMappingNotFoundException {
        assertEquals(FEDORA_ID_PREFIX + "/" + expectedOcflId,
                index.getMapping(null, resourceId).getOcflObjectId());
    }

    private void createResource(final PersistentStorageSession session,
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

    private void createChildResource(final PersistentStorageSession session,
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
}
