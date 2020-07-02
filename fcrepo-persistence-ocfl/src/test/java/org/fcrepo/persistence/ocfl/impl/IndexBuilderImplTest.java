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
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.api.operations.CreateResourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.RdfSourceOperation;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.fcrepo.persistence.ocfl.api.IndexBuilder;
import org.fcrepo.search.api.SearchIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.System.currentTimeMillis;
import static org.fcrepo.kernel.api.operations.ResourceOperationType.CREATE;
import static org.fcrepo.persistence.ocfl.impl.OCFLPersistentStorageUtils.createRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
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
    private TransactionManager transactionManager;

    @Mock
    private Transaction transaction;

    @Mock
    private ContainmentIndex containmentIndex;

    @Mock
    private SearchIndex searchIndex;

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

        final var ocflObjectSessionFactory = new DefaultOCFLObjectSessionFactory(staging);
        setField(ocflObjectSessionFactory, "ocflRepository", repository);

        sessionManager = new OCFLPersistentSessionManager();
        setField(sessionManager, "fedoraOcflIndex", index);
        setField(sessionManager, "objectSessionFactory", ocflObjectSessionFactory);

        indexBuilder = new IndexBuilderImpl();
        setField(indexBuilder, "ocflRepository", repository);
        setField(indexBuilder, "fedoraToOCFLObjectIndex", index);
        setField(indexBuilder, "objectSessionFactory", ocflObjectSessionFactory);
        setField(indexBuilder, "containmentIndex", containmentIndex);
        setField(indexBuilder, "transactionManager", transactionManager);
        setField(indexBuilder, "searchIndex", searchIndex);

        when(transaction.getId()).thenReturn("tx-id");
        when(transactionManager.create()).thenReturn(transaction);
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

        verify(transaction).getId();
        verify(transactionManager).create();
        verify(containmentIndex).addContainedBy(transaction.getId(), FedoraId.getRepositoryRootId(), resource1);
        verify(containmentIndex).addContainedBy(transaction.getId(), resource1, resource2);
        verify(containmentIndex).commitTransaction(transaction);
        verify(searchIndex, times(2)).addUpdateIndex(isA(ResourceHeaders.class));
    }

    @Test
    public void rebuildWhenRepoContainsNonArchivalGroupObject() throws Exception {
        final var session = sessionManager.getSession(session1Id);

        createResource(session, resource1, false);
        createChildResource(session, resource1, resource2);

        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1_resource2", resource2);

        index.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        indexBuilder.rebuildIfNecessary();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1_resource2", resource2);

        verify(transaction).getId();
        verify(transactionManager).create();
        verify(containmentIndex).addContainedBy(transaction.getId(), FedoraId.getRepositoryRootId(), resource1);
        verify(containmentIndex).addContainedBy(transaction.getId(), resource1, resource2);
        verify(containmentIndex).commitTransaction(transaction);
        verify(searchIndex, times(2)).addUpdateIndex(isA(ResourceHeaders.class));
    }

    private void assertDoesNotHaveOcflId(final FedoraId resourceId) {
        try {
            index.getMapping(resourceId.getResourceId());
            fail(resourceId + " should not exist in index");
        } catch (final FedoraOCFLMappingNotFoundException e) {
            //do nothing - expected
        }
    }

    private void assertHasOcflId(final String expectedOcflId, final FedoraId resourceId)
            throws FedoraOCFLMappingNotFoundException {
        assertEquals(expectedOcflId, index.getMapping(resourceId.getResourceId()).getOcflObjectId());
    }

    private void createResource(final PersistentStorageSession session,
                                final FedoraId resourceId, final boolean isArchivalGroup)
            throws PersistentStorageException {
        final var operation = mock(RdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(resourceId.getResourceId());
        when(operation.getType()).thenReturn(CREATE);
        when(((CreateResourceOperation)operation).isArchivalGroup()).thenReturn(isArchivalGroup);
        session.persist(operation);
    }

    private void createChildResource(final PersistentStorageSession session,
                                     final FedoraId parentId, final FedoraId childId)
            throws PersistentStorageException {
        final var operation = mock(NonRdfSourceOperation.class, withSettings().extraInterfaces(
                CreateResourceOperation.class));
        when(operation.getResourceId()).thenReturn(childId.getResourceId());
        when(operation.getType()).thenReturn(CREATE);
        final var bytes = "test".getBytes();
        final var stream = new ByteArrayInputStream(bytes);
        when(operation.getContentSize()).thenReturn((long)bytes.length);
        when(operation.getContentStream()).thenReturn(stream);
        when(operation.getMimeType()).thenReturn("text/plain");
        when(operation.getFilename()).thenReturn("test");
        when(((CreateResourceOperation)operation).getParentId()).thenReturn(parentId.getResourceId());
        session.persist(operation);
    }

}
