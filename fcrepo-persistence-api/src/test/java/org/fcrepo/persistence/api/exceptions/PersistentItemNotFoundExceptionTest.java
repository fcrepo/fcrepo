/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class PersistentItemNotFoundExceptionTest {
    private static final String TEST_MESSAGE = "Test error message";

    @Test
    public void testSingleArgumentConstructor() {
        final var exception = new PersistentItemNotFoundException(TEST_MESSAGE);

        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testTwoArgumentConstructor() {
        final Throwable cause = new Exception("Original cause");
        final var exception = new PersistentItemNotFoundException(TEST_MESSAGE, cause);

        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testExceptionHierarchy() {
        assertEquals(PersistentStorageException.class, PersistentItemNotFoundException.class.getSuperclass());
    }
}
