/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.api.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class ObjectExistsInOcflIndexExceptionTest {

    private static final String TEST_MESSAGE = "Test error message";

    @Test
    public void testSingleArgumentConstructor() {
        final ObjectExistsInOcflIndexException exception = new ObjectExistsInOcflIndexException(TEST_MESSAGE);

        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testTwoArgumentConstructor() {
        final Throwable cause = new Exception("Original cause");
        final ObjectExistsInOcflIndexException exception =
                new ObjectExistsInOcflIndexException(TEST_MESSAGE, cause);

        assertEquals(TEST_MESSAGE, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testExceptionHierarchy() {
        final ObjectExistsInOcflIndexException exception = new ObjectExistsInOcflIndexException(TEST_MESSAGE);

        // Assert that this exception inherits from RepositoryRuntimeException
        assertEquals(RepositoryRuntimeException.class, exception.getClass().getSuperclass());
    }
}