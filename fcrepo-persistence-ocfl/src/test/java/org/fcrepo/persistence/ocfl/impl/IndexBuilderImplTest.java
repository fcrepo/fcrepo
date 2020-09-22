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

import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
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
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.System.currentTimeMillis;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.DELETE;
import static org.fcrepo.persistence.ocfl.impl.OcflPersistentStorageUtils.createRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author dbernstein
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class IndexBuilderImplTest {

    private PersistentStorageSessionManager sessionManager;
    private FedoraToOcflObjectIndex index;
    private IndexBuilder indexBuilder;

    @Mock
    private ContainmentIndex containmentIndex;

    @Mock
    private SearchIndex searchIndex;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private MembershipService membershipService;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final String session1Id = "session1";
    private final FedoraId resource1 = FedoraId.create("info:fedora/resource1");
    private final FedoraId resource2 =  FedoraId.create(resource1 + "/resource2");

    @Before
    public void setup() throws IOException {
        final var targetDir = Paths.get("target");
        final var dataDir = targetDir.resolve("test-fcrepo-data-" + currentTimeMillis());
        final var repoDir = dataDir.resolve("ocfl-repo");
        final var workDir = dataDir.resolve("ocfl-work");
        final var staging = dataDir.resolve("ocfl-staging");

        final var repository = createRepository(repoDir, workDir);

        index = new TestOcflObjectIndex();
        index.reset();

        final var objectMapper = OcflPersistentStorageUtils.objectMapper();
        final var ocflObjectSessionFactory = new DefaultOcflObjectSessionFactory(repository,
                tempFolder.newFolder().toPath(),
                objectMapper, CommitType.NEW_VERSION,
                "Fedora 6 test", "fedoraAdmin", "info:fedora/fedoraAdmin");

        sessionManager = new OcflPersistentSessionManager();
        setField(sessionManager, "fedoraOcflIndex", index);
        setField(sessionManager, "objectSessionFactory", ocflObjectSessionFactory);

        indexBuilder = new IndexBuilderImpl();
        setField(indexBuilder, "ocflRepository", repository);
        setField(indexBuilder, "fedoraToOcflObjectIndex", index);
        setField(indexBuilder, "objectSessionFactory", ocflObjectSessionFactory);
        setField(indexBuilder, "containmentIndex", containmentIndex);
        setField(indexBuilder, "searchIndex", searchIndex);
        setField(indexBuilder, "referenceService", referenceService);
        setField(indexBuilder, "membershipService", membershipService);
    }

    @Test
    public void rebuildWhenRepoContainsArchivalGroupObject() throws Exception {
        final var session = sessionManager.getSession(session1Id);

        createResource(session, resource1, true);
        createChildResource(session, resource1, resource2);

        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        index.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        indexBuilder.rebuildIfNecessary();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        verify(containmentIndex).addContainedBy(anyString(), eq(FedoraId.getRepositoryRootId()), eq(resource1));
        verify(containmentIndex).addContainedBy(anyString(), eq(resource1), eq(resource2));
        verify(containmentIndex).commitTransaction(anyString());
        verify(searchIndex, times(2)).addUpdateIndex(isA(String.class), isA(ResourceHeaders.class));
    }

    @Test
    public void rebuildWhenRepoContainsNonArchivalGroupObject() throws Exception {
        final var session = sessionManager.getSession(session1Id);

        createResource(session, resource1, false);
        createChildResource(session, resource1, resource2);

        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1/resource2", resource2);

        index.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        indexBuilder.rebuildIfNecessary();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1/resource2", resource2);

        verify(containmentIndex).addContainedBy(anyString(), eq(FedoraId.getRepositoryRootId()), eq(resource1));
        verify(containmentIndex).addContainedBy(anyString(), eq(resource1), eq(resource2));
        verify(containmentIndex).commitTransaction(anyString());
        verify(searchIndex, times(2)).addUpdateIndex(isA(String.class), isA(ResourceHeaders.class));
    }

    @Test
    public void shouldNotAddDeletedResourcesToContainmentIndex() throws PersistentStorageException,
            FedoraOcflMappingNotFoundException {
        final var session = sessionManager.getSession(session1Id);

        createResource(session, resource1, true);
        createChildResource(session, resource1, resource2);

        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        final var session2 = sessionManager.getSession("session2");

        deleteResource(session2, resource2);

        session2.commit();

        index.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        indexBuilder.rebuildIfNecessary();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        verify(containmentIndex).addContainedBy(anyString(), eq(FedoraId.getRepositoryRootId()), eq(resource1));
        verify(containmentIndex, never()).addContainedBy(anyString(), eq(resource1), eq(resource2));
        verify(containmentIndex).commitTransaction(anyString());
        verify(searchIndex, times(1)).addUpdateIndex(anyString(), isA(ResourceHeaders.class));
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
        assertEquals(FedoraTypes.FEDORA_ID_PREFIX + "/" + expectedOcflId,
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
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(BASIC_CONTAINER.toString());
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
        when(((CreateResourceOperation) operation).getInteractionModel()).thenReturn(NON_RDF_SOURCE.toString());
        session.persist(operation);
    }

    private void deleteResource(final PersistentStorageSession session, final FedoraId resourceId)
            throws PersistentStorageException {
        final var operation = mock(DeleteResourceOperation.class);
        when(operation.getResourceId()).thenReturn(resourceId);
        when(operation.getType()).thenReturn(DELETE);
        session.persist(operation);
    }

}
