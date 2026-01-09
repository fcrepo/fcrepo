/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.ocfl.api.OcflRepository;
import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.kernel.impl.models.ResourceFactoryImpl;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.storage.ocfl.exception.ValidationException;
import org.fcrepo.storage.ocfl.validation.ObjectValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * ReindexService tests.
 * @author dbernstein
 * @author whikloj
 * @since 6.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReindexServiceTest extends AbstractReindexerTest {

    private ReindexManager reindexManager;

    private ReindexService reindexService;

    @Mock
    private ObjectValidator objectValidator;

    @Mock
    private OcflRepository ocflRepository;

    @Mock
    private FedoraPropsConfig fedoraConfig;

    private ResourceFactory resourceFactory;

    @Mock
    private RepositoryInitializationStatus initializationStatus;

    private final FedoraId resource1 = FedoraId.create("info:fedora/resource1");
    private final FedoraId resource2 =  FedoraId.create(resource1 + "/resource2");

    @BeforeEach
    public void setup() throws Exception {
        super.setup();

        resourceFactory = new ResourceFactoryImpl();
        setField(resourceFactory, "containmentIndex", containmentIndex);
        setField(resourceFactory, "persistentStorageSessionManager", persistentStorageSessionManager);

        reindexService = new ReindexService();
        reindexService.setMembershipPageSize(5);
        setField(reindexService, "membershipService", membershipService);
        setField(reindexService, "referenceService", referenceService);
        setField(reindexService, "resourceFactory", resourceFactory);
        setField(reindexService, "searchIndex", searchIndex);
        setField(reindexService, "containmentIndex", containmentIndex);
        setField(reindexService, "ocflIndex", ocflIndex);
        setField(reindexService, "ocflRepository", ocflRepository);
        setField(reindexService, "ocflObjectSessionFactory", ocflObjectSessionFactory);
        setField(reindexService, "persistentStorageSessionManager", persistentStorageSessionManager);
        setField(reindexService, "objectValidator", objectValidator);
        setField(reindexService, "config", fedoraConfig);
        setField(reindexService, "initializationStatus", initializationStatus);
        when(searchIndex.doSearch(any(SearchParameters.class))).thenReturn(containerResult);


        when(propsConfig.getReindexingThreads()).thenReturn(2L);
        when(fedoraConfig.isRebuildValidation()).thenReturn(true);
        reindexManager = getReindexManager();

        doAnswer(invocationOnMock -> {
            // Consume the RdfStream to simulate processing.
            invocationOnMock.getArgument(3, RdfStream.class).count();
            return null;
        }).when(referenceService).updateReferences(any(Transaction.class), any(FedoraId.class),
                isNull(), any(RdfStream.class));
    }

    /**
     * @return Get a new ReindexManager.
     */
    private ReindexManager getReindexManager() {
        return new ReindexManager(repository.listObjectIds(),
                reindexService, propsConfig, txManager, new DbTransactionExecutor());
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
        verify(searchIndex, times(2)).addUpdateIndex(
                any(Transaction.class), any(org.fcrepo.kernel.api.models.ResourceHeaders.class), anyList());

        verify(transaction, times(2)).commit();
        verify(searchIndex, times(2)).doSearch(any(SearchParameters.class));
    }

    @Test
    public void testRebuildDuringInitialization() throws Exception {
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

        // Trigger reindex during in initialization mode, so already indexed entries are skipped.
        clearInvocations(containmentIndex, referenceService, searchIndex);
        when(initializationStatus.isInitializationComplete()).thenReturn(false);
        reindexManager.start();
        reindexManager.shutdown();

        assertHasOcflId(parentIdPart, parentId);
        assertHasOcflId(parentIdPart, childId);

        verify(containmentIndex, never()).addContainedBy(any(Transaction.class), any(FedoraId.class),
                any(FedoraId.class), any(Instant.class), isNull());
        verify(referenceService, never()).updateReferences(any(Transaction.class), any(FedoraId.class),
                isNull(), any(RdfStream.class));
        verify(searchIndex, never())
                .addUpdateIndex(any(Transaction.class), any(org.fcrepo.kernel.api.models.ResourceHeaders.class));
    }

    @Test
    public void testRepeatRebuildAfterInitialization() throws Exception {
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

        // Run the reindex after initialization is complete, so existing entries are reindexed.
        when(initializationStatus.isInitializationComplete()).thenReturn(true);
        reindexManager.start();
        reindexManager.shutdown();

        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(FedoraId.getRepositoryRootId()),
                eq(parentId), any(Instant.class), isNull());
        verify(containmentIndex).addContainedBy(any(Transaction.class), eq(parentId), eq(childId), any(Instant.class),
                isNull());
        verify(referenceService).updateReferences(any(Transaction.class), eq(childId), isNull(), any(RdfStream.class));
        verify(searchIndex, times(2)).addUpdateIndex(
                any(Transaction.class), any(org.fcrepo.kernel.api.models.ResourceHeaders.class), anyList());

        verify(transaction, times(2)).commit();
        verify(searchIndex, times(2)).doSearch(any(SearchParameters.class));
    }

    @Test
    public void testRebuildWithContinue() throws Exception {
        final var session = persistentStorageSessionManager.getSession(transaction);

        final int numberContainers = 25;
        final Map<String, FedoraId> result = new HashMap<>();
        for (int i = 0; i < numberContainers; i++) {
            final var id = UUID.randomUUID().toString();
            final FedoraId containerId = FedoraId.create(id);
            result.put(id, containerId);
            createResource(session, containerId, false);
        }

        session.prepare();
        session.commit();

        for (final var idMap : result.entrySet()) {
            assertHasOcflId(idMap.getKey(), idMap.getValue());
        }

        ocflIndex.reset();

        for (final var idMap : result.entrySet()) {
            assertDoesNotHaveOcflId(idMap.getValue());
        }

        // Fail the validation on a random object.
        final var random = new Random();
        final var idList = new ArrayList<>(result.values());
        final var randomId = idList.get(random.nextInt(idList.size()));
        doThrow(ValidationException.create(List.of("validation errors")))
                .when(objectValidator).validate(randomId.getFullId(), false);

        reindexManager.start();
        reindexManager.shutdown();

        final Map<String, FedoraId> missingResults = new HashMap<>();

        for (final var idMap : result.entrySet()) {
            try {
                assertHasOcflId(idMap.getKey(), idMap.getValue());
            } catch (final FedoraOcflMappingNotFoundException e) {
                missingResults.put(idMap.getKey(), idMap.getValue());
            }
        }

        assertThat(missingResults.size(), greaterThan(0));

        // Clear the exception
        Mockito.reset(objectValidator);

        // Rerun the reindex with continue set.
        when(fedoraConfig.isRebuildContinue()).thenReturn(true);
        // Get a new ReindexManager to regenerate the worker threads.
        reindexManager = getReindexManager();
        reindexManager.start();
        reindexManager.shutdown();

        // Assert all the results are indexed.
        for (final var idMap : result.entrySet()) {
            assertHasOcflId(idMap.getKey(), idMap.getValue());
        }

        verify(containmentIndex, times(numberContainers)).addContainedBy(any(Transaction.class),
                eq(FedoraId.getRepositoryRootId()), any(FedoraId.class), any(Instant.class), isNull());
        verify(searchIndex, times(numberContainers)).addUpdateIndex(
                any(Transaction.class), any(org.fcrepo.kernel.api.models.ResourceHeaders.class), anyList());

        verify(transaction, times(numberContainers + 1)).commit();
        verify(searchIndex, times(2)).doSearch(any(SearchParameters.class));
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
                org.fcrepo.kernel.api.models.ResourceHeaders.class), anyList());
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
                isA(ResourceHeaders.class), anyList());
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
        verify(searchIndex, times(1)).addUpdateIndex(
                any(Transaction.class), isA(ResourceHeaders.class), anyList());
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
                org.fcrepo.kernel.api.models.ResourceHeaders.class), anyList());
        verify(membershipService, times(numberContainers * 2)).populateMembershipHistory(any(Transaction.class),
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

    @Test
    public void testResetReindexService() {
        reindexService.reset();
        // OcflIndex is not a mock, so we can't verify it
        verify(containmentIndex).reset();
        verify(referenceService).reset();
        verify(searchIndex).reset();
        verify(membershipService).reset();
    }
}
