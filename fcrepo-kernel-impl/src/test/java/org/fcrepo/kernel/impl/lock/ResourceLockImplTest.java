/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.lock.ResourceLock;
import org.fcrepo.kernel.api.lock.ResourceLockType;

import org.junit.Before;
import org.junit.Test;

/**
 * Test ResourceLock
 * @author whikloj
 */
public class ResourceLockImplTest {

    private String txId;
    private FedoraId resourceId;

    @Before
    public void setUp() {
        txId = "tx" + UUID.randomUUID();
        resourceId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void testExclusiveLockString() {
        final ResourceLockImpl newLock = new ResourceLockImpl("exclusive", txId, resourceId);
        doLockChecks(newLock, ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void testExclusiveLock() {
        final ResourceLockImpl newLock = new ResourceLockImpl(ResourceLockType.EXCLUSIVE, txId, resourceId);
        doLockChecks(newLock, ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void testNonExclusiveLockString() {
        final ResourceLockImpl newLock = new ResourceLockImpl("non-exclusive", txId, resourceId);
        doLockChecks(newLock, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void testNonExclusiveLock() {
        final ResourceLockImpl newLock = new ResourceLockImpl(ResourceLockType.NONEXCLUSIVE, txId, resourceId);
        doLockChecks(newLock, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void testInvalidLockType() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceLockImpl("create", txId, resourceId));
    }

    private void doLockChecks(final ResourceLock newLock, final ResourceLockType expectedType) {
        assertEquals(expectedType, newLock.getLockType());
        assertEquals(txId, newLock.getTransactionId());
        assertEquals(resourceId, newLock.getResourceId());
        assertTrue(newLock.hasLockType(expectedType));
        assertTrue(newLock.hasResource(resourceId));
    }
}
