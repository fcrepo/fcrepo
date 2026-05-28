/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class WebacAclImplTest {

    @Mock
    private Transaction mockTransaction;

    @Mock
    private PersistentStorageSessionManager mockPsManager;

    @Mock
    private ResourceFactory mockResourceFactory;

    @Mock
    private UserTypesCache mockUserTypesCache;

    @Mock
    private FedoraResource mockResource;

    private WebacAclImpl webacAcl;
    private final FedoraId fedoraId = FedoraId.create("info:fedora/test/resource/fcr:acl");

    @BeforeEach
    public void setUp() {
        webacAcl = new WebacAclImpl(fedoraId, mockTransaction, mockPsManager,
                mockResourceFactory, mockUserTypesCache);
    }

    @Test
    public void testGetContainer() throws Exception {
        when(mockResourceFactory.getResource(any(Transaction.class), any(FedoraId.class)))
                .thenReturn(mockResource);

        final FedoraResource result = webacAcl.getContainer();

        assertEquals(mockResource, result);
    }

    @Test
    public void testGetContainerNotFound() throws Exception {
        when(mockResourceFactory.getResource(any(Transaction.class), any(FedoraId.class)))
                .thenThrow(new PathNotFoundException("Resource not found"));

        assertThrows(PathNotFoundRuntimeException.class, () -> webacAcl.getContainer());
    }

    @Test
    public void testIsOriginalResource() {
        assertFalse(webacAcl.isOriginalResource());
    }

    @Test
    public void testIsAcl() {
        assertTrue(webacAcl.isAcl());
    }
}