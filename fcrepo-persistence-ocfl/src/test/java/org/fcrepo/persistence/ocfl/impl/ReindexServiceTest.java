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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.storage.ocfl.exception.ValidationException;
import org.fcrepo.storage.ocfl.validation.ObjectValidator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * ReindexService tests.
 * @author dbernstein
 * @author whikloj
 * @since 6.0.0
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReindexServiceTest extends AbstractReindexerTest {

    private ReindexManager reindexManager;

    private ReindexService reindexService;

    @Mock
    private ObjectValidator objectValidator;

    @Mock
    private FedoraPropsConfig fedoraConfig;

    private final FedoraId resource1 = FedoraId.create("info:fedora/resource1");
    private final FedoraId resource2 =  FedoraId.create(resource1 + "/resource2");

    @Before
    public void setup() throws Exception {
        super.setup();

        reindexService = new ReindexService();
        reindexService.setMembershipPageSize(5);
        setField(reindexService, "membershipService", membershipService);
        setField(reindexService, "referenceService", referenceService);
        setField(reindexService, "searchIndex", searchIndex);
        setField(reindexService, "containmentIndex", containmentIndex);
        setField(reindexService, "ocflIndex", ocflIndex);
        setField(reindexService, "ocflObjectSessionFactory", ocflObjectSessionFactory);
        setField(reindexService, "persistentStorageSessionManager", persistentStorageSessionManager);
        setField(reindexService, "objectValidator", objectValidator);
        setField(reindexService, "config", fedoraConfig);
        when(searchIndex.doSearch(any(SearchParameters.class))).thenReturn(containerResult);


        when(propsConfig.getReindexingThreads()).thenReturn(2L);
        reindexManager = new ReindexManager(repository.listObjectIds(), reindexService, propsConfig, txManager);
    }

    @Test
    public void testRebuildOnce() throws Exception {
        final String parentIdPart = getRandomId();
        final String childIdPart = getRandomId();
        final var parentId = FedoraId.create(parentIdPart);
        final var childId = parentId.resolve(childIdPart);
        final var session = persistentStorageSessionManager.getSession(transaction);

        createResource(session, parentId, true);
        createChildResourceRdf(session, parentId, childId);

        session.prepare();
        session.commit();

        assertHasOcflId(parentIdPart, parentId);
        assertHasOcflId(parentIdPart, childId);

        ocflIndex.reset();

        assertDoesNotHaveOcflId(parentId);
        assertDoesNotHaveOcflId(childId);

        reindexManager.start();
        reindexManager.shutdown();

        assertHasOcflId(parentIdPart, parentId);
        assertHasOcflId(parentIdPart, childId);

        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(FedoraId.getRepositoryRootId()),
                eq(parentId), any(Instant.class), isNull());
        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(parentId), eq(childId), any(Instant.class),
                isNull());
        verify(referenceService).updateReferences(any(Transaction.class), eq(childId), isNull(), any(RdfStream.class));
        verify(searchIndex, times(2))
                .addUpdateIndex(any(Transaction.class), any(org.fcrepo.kernel.api.models.ResourceHeaders.class));

        verify(transaction, times(2)).commit();
        verify(searchIndex).doSearch(any(SearchParameters.class));
    }

    @Test
    public void rebuildWhenRepoContainsArchivalGroupObject() throws Exception {
        final var session = persistentStorageSessionManager.getSession(transaction);

        createResource(session, resource1, true);
        createChildResourceNonRdf(session, resource1, resource2);

        session.prepare();
        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        ocflIndex.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        reindexManager.start();
        reindexManager.shutdown();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(FedoraId.getRepositoryRootId()),
                eq(resource1), any(Instant.class), isNull());
        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(resource1), eq(resource2),
                any(Instant.class), isNull());
        verify(searchIndex, times(2)).addUpdateIndex(any(Transaction.class), isA(
                org.fcrepo.kernel.api.models.ResourceHeaders.class));
        verify(transaction, times(2)).commit();
    }

    @Test
    public void rebuildWhenRepoContainsNonArchivalGroupObject() throws Exception {
        final var session = persistentStorageSessionManager.getSession(transaction);

        createResource(session, resource1, false);
        createChildResourceNonRdf(session, resource1, resource2);

        session.prepare();
        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1/resource2", resource2);

        ocflIndex.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        reindexManager.start();
        reindexManager.shutdown();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1/resource2", resource2);

        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(FedoraId.getRepositoryRootId()),
                eq(resource1), any(Instant.class), isNull());
        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(resource1), eq(resource2),
                any(Instant.class), isNull());
        verify(transaction, times(3)).commit();
        verify(searchIndex, times(2)).addUpdateIndex(any(Transaction.class),
                isA(ResourceHeaders.class));
    }

    @Test
    public void shouldNotAddDeletedResourcesToContainmentIndex() throws Exception {
        final var session = persistentStorageSessionManager.getSession(transaction);

        createResource(session, resource1, true);
        createChildResourceNonRdf(session, resource1, resource2);

        session.prepare();
        session.commit();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        final var transaction2 = mock(Transaction.class);
        when(transaction2.getId()).thenReturn("session2");
        final var session2 = persistentStorageSessionManager.getSession(transaction2);

        deleteResource(session2, resource2);

        session2.prepare();
        session2.commit();

        ocflIndex.reset();

        assertDoesNotHaveOcflId(resource1);
        assertDoesNotHaveOcflId(resource2);

        reindexManager.start();
        reindexManager.shutdown();

        assertHasOcflId("resource1", resource1);
        assertHasOcflId("resource1", resource2);

        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(FedoraId.getRepositoryRootId()),
                eq(resource1), any(Instant.class), isNull());
        verify(containmentIndex, never()).addContainedBy(any(Transaction.class), eq(resource1), eq(resource2),
                any(Instant.class), isNull());
        verify(transaction, times(2)).commit();
        verify(searchIndex, times(1)).addUpdateIndex(any(Transaction.class), isA(ResourceHeaders.class));
    }

    // Verify that DirectContainers get membership rebuilt, and that querying/paging for resources works
    @Test
    public void rebuildRepoLotsOfMembership() throws Exception {
        // Reduce the page size so the test doesn't take a while
        reindexService.setMembershipPageSize(5);

        final var session = persistentStorageSessionManager.getSession(transaction);

        final int numberContainers = 18;
        final List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < numberContainers; i++) {
            final FedoraId containerId = FedoraId.create(UUID.randomUUID().toString());
            result.add(Map.of(Condition.Field.FEDORA_ID.toString(), containerId.getFullId()));
            createResource(session, containerId, false);
        }

        when(containerResult.getItems()).thenReturn(result);

        session.prepare();
        session.commit();

        ocflIndex.reset();

        reindexManager.start();
        reindexManager.shutdown();

        verify(containmentIndex, times(numberContainers)).addContainedBy(any(Transaction.class),
                eq(FedoraId.getRepositoryRootId()), any(FedoraId.class), any(Instant.class), isNull());
        verify(transaction, times(numberContainers + 1)).commit();
        verify(searchIndex, times(numberContainers)).addUpdateIndex(any(Transaction.class), isA(
                org.fcrepo.kernel.api.models.ResourceHeaders.class));
        verify(membershipService, times(numberContainers)).populateMembershipHistory(any(Transaction.class),
                any(FedoraId.class));
    }

    @Test
    public void failRebuildWhenObjectFailsValidation() throws Exception {
        final String parentIdPart = getRandomId();
        final String childIdPart = getRandomId();
        final var parentId = FedoraId.create(parentIdPart);
        final var childId = parentId.resolve(childIdPart);
        final var session = persistentStorageSessionManager.getSession(transaction);

        createResource(session, parentId, true);
        createChildResourceRdf(session, parentId, childId);

        session.prepare();
        session.commit();

        assertHasOcflId(parentIdPart, parentId);
        assertHasOcflId(parentIdPart, childId);

        ocflIndex.reset();

        assertDoesNotHaveOcflId(parentId);
        assertDoesNotHaveOcflId(childId);

        doThrow(ValidationException.create(List.of("validation errors")))
                .when(objectValidator).validate(parentId.getFullId(), false);

        reindexManager.start();
        reindexManager.shutdown();

        assertDoesNotHaveOcflId(parentId);
        assertDoesNotHaveOcflId(childId);
    }

}
