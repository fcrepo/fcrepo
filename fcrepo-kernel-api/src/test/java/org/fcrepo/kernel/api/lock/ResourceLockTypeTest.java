/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link ResourceLockType}
 *
 * @author whikloj
 */
public class ResourceLockTypeTest {

    @Test
    public void testResourceLockType() {
        // Test the enum values
        assertEquals("exclusive", ResourceLockType.EXCLUSIVE.getValue());
        assertEquals("non-exclusive", ResourceLockType.NONEXCLUSIVE.getValue());

        // Test the enum values with different cases
        assertEquals(ResourceLockType.EXCLUSIVE, ResourceLockType.fromString("exclusive"));
        assertEquals(ResourceLockType.EXCLUSIVE, ResourceLockType.fromString("Exclusive"));

        assertEquals(ResourceLockType.NONEXCLUSIVE, ResourceLockType.fromString("non-exclusive"));
        assertEquals(ResourceLockType.NONEXCLUSIVE, ResourceLockType.fromString("Non-Exclusive"));

        // Test the exception for invalid value
        assertThrows(IllegalArgumentException.class, () -> ResourceLockType.fromString("some-invalid-value"));
    }
}
