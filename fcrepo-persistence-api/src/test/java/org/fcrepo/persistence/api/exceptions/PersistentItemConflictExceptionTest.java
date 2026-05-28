/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class PersistentItemConflictExceptionTest {
    private static final String TEST_MESSAGE = "Test error message";

    @Test
    public void testSingleArgumentConstructor() {
        final var exception = new PersistentItemConflictException(TEST_MESSAGE);

        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testExceptionHierarchy() {
        assertEquals(PersistentStorageException.class, PersistentItemConflictException.class.getSuperclass());
    }
}
