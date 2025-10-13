/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ocfl.api.OcflRepository;
import org.fcrepo.common.db.DbTransactionExecutor;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class IndexBuilderImplTest {

    @Mock
    private FedoraToOcflObjectIndex ocflIndex;

    @Mock
    private ContainmentIndex containmentIndex;

    @Mock
    private OcflRepository ocflRepository;

    @Mock
    private ReindexService reindexService;

    @Mock
    private OcflPropsConfig ocflPropsConfig;

    @Mock
    private FedoraPropsConfig fedoraPropsConfig;

    @Mock
    private TransactionManager txManager;

    @Mock
    private Transaction transaction;

    @Mock
    private DbTransactionExecutor dbTransactionExecutor;

    @InjectMocks
    private IndexBuilderImpl indexBuilder;

    @Mock
    private FedoraOcflMapping rootMapping;

    private static final String ROOT_OBJECT_ID = "root-object";

    @BeforeEach
    public void setup() {
        when(ocflPropsConfig.getReindexingThreads()).thenReturn(1L);
        when(txManager.create()).thenReturn(transaction);
    }

    @Test
    public void testRebuildIfNecessary_NoRebuildNeeded_RootPresent() throws Exception {
        // Configure root object to exist
        when(ocflIndex.getMapping(ReadOnlyTransaction.INSTANCE, FedoraId.getRepositoryRootId()))
                .thenReturn(rootMapping);
        when(rootMapping.getOcflObjectId()).thenReturn(ROOT_OBJECT_ID);
        when(ocflRepository.containsObject(ROOT_OBJECT_ID)).thenReturn(true);
        when(fedoraPropsConfig.isRebuildContinue()).thenReturn(false);

        try (final var mockReindexManagers = Mockito.mockConstruction(ReindexManager.class)) {
            indexBuilder.rebuildIfNecessary();

            // Verify no rebuild was performed
            verify(reindexService, never()).reset();
            assertTrue(mockReindexManagers.constructed().isEmpty());
        }
    }

    @Test
    public void testRebuildIfNecessary_NoRebuildNeeded_NoRoot() throws Exception {
        // Configure root object to exist
        when(ocflIndex.getMapping(ReadOnlyTransaction.INSTANCE, FedoraId.getRepositoryRootId()))
                .thenThrow(new FedoraOcflMappingNotFoundException("Missing"));
        when(ocflRepository.containsObject(ROOT_OBJECT_ID)).thenReturn(false);
        when(fedoraPropsConfig.isRebuildContinue()).thenReturn(false);

        // No errors, but no reindex either
        try (final var mockReindexManagers = Mockito.mockConstruction(ReindexManager.class)) {
            indexBuilder.rebuildIfNecessary();

            // Verify no rebuild was performed
            verify(reindexService, never()).reset();
            assertTrue(mockReindexManagers.constructed().isEmpty());
        }
    }

    @Test
    public void testRebuildIfNecessary_RootObjectNotInOcflButIndexed() throws Exception {
        // Configure root mapping exists, but object doesn't
        when(ocflIndex.getMapping(ReadOnlyTransaction.INSTANCE, FedoraId.getRepositoryRootId()))
                .thenReturn(rootMapping);
        when(rootMapping.getOcflObjectId()).thenReturn(ROOT_OBJECT_ID);
        when(ocflRepository.containsObject(ROOT_OBJECT_ID)).thenReturn(false);

        try (final var mockReindexManagers = Mockito.mockConstruction(ReindexManager.class)) {
            indexBuilder.rebuildIfNecessary();

            // Verify no rebuild was performed
            verify(reindexService, never()).reset();
            assertTrue(mockReindexManagers.constructed().isEmpty());
        }
    }

    @Test
    public void testRebuildIfNecessary_RebuildContinueEnabled() throws Exception {
        when(fedoraPropsConfig.isRebuildContinue()).thenReturn(true);

        // Setup some test object IDs
        mockObjectIds(List.of("obj1", "obj2"));

        try (final var mockReindexManagers = Mockito.mockConstruction(ReindexManager.class)) {
            indexBuilder.rebuildIfNecessary();

            // Verify rebuild was performed but reset was not called
            verify(reindexService, never()).reset();
            final var mockReindexManager = mockReindexManagers.constructed().get(0);
            verify(mockReindexManager).start();
            verify(mockReindexManager).shutdown();
        }
    }

    @Test
    public void testGetDurationMessage() throws Exception {
        // Test the duration message formatting via reflection
        final var getDurationMessage = IndexBuilderImpl.class.getDeclaredMethod(
                "getDurationMessage", java.time.Duration.class);
        getDurationMessage.setAccessible(true);

        // Test seconds only
        final String secondsMessage = (String) getDurationMessage.invoke(
                indexBuilder, java.time.Duration.ofSeconds(42));
        assertEquals("42 seconds", secondsMessage);

        // Test minutes and seconds
        final String minutesMessage = (String) getDurationMessage.invoke(
                indexBuilder, java.time.Duration.ofSeconds(125));
        assertEquals("2 mins, 5 seconds", minutesMessage);

        // Test hours, minutes, seconds
        final String hoursMessage = (String) getDurationMessage.invoke(
                indexBuilder, java.time.Duration.ofSeconds(3725));
        assertEquals("1 hours, 2 mins, 5 seconds", hoursMessage);
    }

    private void mockObjectIds(final List<String> objectIds) {
        // Create a mock iterator to return the provided object IDs
        when(ocflRepository.listObjectIds()).thenReturn(objectIds.stream());
    }
}