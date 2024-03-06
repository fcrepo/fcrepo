/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fcrepo.kernel.api.exception.InsufficientStorageException;

import jakarta.ws.rs.core.Response;

/**
 * <p>
 * InvalidChecksumExceptionMapperTest class.
 * </p>
 *
 * @author Daniel Bernstein
 * @since Oct 7, 2016
 */
public class InsufficientStorageExceptionMapperTest {

    private InsufficientStorageExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new InsufficientStorageExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final InsufficientStorageException input = new InsufficientStorageException(
                "No space left on device.", null);
        try (final Response actual = testObj.toResponse(input)) {
            assertEquals(InsufficientStorageExceptionMapper.INSUFFICIENT_STORAGE_HTTP_CODE, actual.getStatus());
        }
    }
}
