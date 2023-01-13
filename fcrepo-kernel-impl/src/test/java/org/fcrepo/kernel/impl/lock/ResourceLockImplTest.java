/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.lock;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

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
    private String resourceId;

    @Before
    public void setUp() {
        txId = "tx" + UUID.randomUUID();
        resourceId = FEDORA_ID_PREFIX + "/" + UUID.randomUUID();
    }

    @Test
    public void testExclusiveLockString() {
        testLockString("exclusive", ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void testExclusiveLock() {
        testLockType(ResourceLockType.EXCLUSIVE, ResourceLockType.EXCLUSIVE);
    }

    @Test
    public void testNonExclusiveLockString() {
        testLockString("non-exclusive", ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void testNonExclusiveLock() {
        testLockType(ResourceLockType.NONEXCLUSIVE, ResourceLockType.NONEXCLUSIVE);
    }

    @Test
    public void testInvalidLockType() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceLockImpl("create", txId, resourceId));
    }

    private void testLockString(final String type, final ResourceLockType expectedType) {
        final ResourceLockImpl newLock = new ResourceLockImpl(type, txId, resourceId);
        doLockChecks(newLock, expectedType);
    }

    private void testLockType(final ResourceLockType lockType, final ResourceLockType expectedType) {
        final ResourceLockImpl newLock = new ResourceLockImpl(lockType, txId, resourceId);
        doLockChecks(newLock, expectedType);
    }

    private void doLockChecks(final ResourceLock newLock, final ResourceLockType expectedType) {
        assertEquals(expectedType, newLock.getLockType());
        assertEquals(txId, newLock.getTransactionId());
        assertEquals(resourceId, newLock.getResourceId());
        assertTrue(newLock.hasLockType(expectedType));
        assertTrue(newLock.hasResource(resourceId));
    }
}
