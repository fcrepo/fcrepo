/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
class TombstoneImplTest {

    @Mock
    private FedoraId mockFedoraId;

    @Mock
    private Transaction mockTransaction;

    @Mock
    private PersistentStorageSessionManager mockPSessionManager;

    @Mock
    private ResourceFactory mockResourceFactory;

    @Mock
    private FedoraResource mockOriginalResource;

    private TombstoneImpl tombstone;

    @BeforeEach
    void setUp() {
        tombstone = new TombstoneImpl(mockFedoraId, mockTransaction, mockPSessionManager,
                mockResourceFactory, mockOriginalResource);
    }

    @Test
    void testGetDeletedObject() {
        final FedoraResource result = tombstone.getDeletedObject();
        assertEquals(mockOriginalResource, result, "Should return the original resource");
    }

    @Test
    void testGetFedoraId() {
        final FedoraId resourceId = FedoraId.create("test/resource");
        when(mockOriginalResource.getFedoraId()).thenReturn(resourceId);

        final FedoraId result = tombstone.getFedoraId();
        assertEquals(resourceId, result, "Should return the original resource's FedoraId");
    }

    @Test
    void testGetUserTypes() {
        final List<URI> result = tombstone.getUserTypes();
        assertTrue(result.isEmpty(), "User types should be empty");
    }

    @Test
    void testGetSystemTypes() {
        final List<URI> resultForRdf = tombstone.getSystemTypes(true);
        assertTrue(resultForRdf.isEmpty(), "System types should be empty when forRdf is true");

        final List<URI> resultNotForRdf = tombstone.getSystemTypes(false);
        assertTrue(resultNotForRdf.isEmpty(), "System types should be empty when forRdf is false");
    }
}