package org.fcrepo.persistence.api.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class PersistentSessionClosedExceptionTest {
    private static final String TEST_MESSAGE = "Test error message";

    @Test
    public void testSingleArgumentConstructor() {
        final var exception = new PersistentSessionClosedException(TEST_MESSAGE);

        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testTwoArgumentConstructor() {
        final Throwable cause = new Exception("Original cause");
        final var exception = new PersistentSessionClosedException(TEST_MESSAGE, cause);

        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testExceptionHierarchy() {
        assertEquals(PersistentStorageException.class, PersistentSessionClosedException.class.getSuperclass());
    }
}
