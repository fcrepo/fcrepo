/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;

import jakarta.ws.rs.core.Response;

/**
 * <p>InvalidChecksumExceptionMapperTest class.</p>
 *
 * @author awoods
 */
public class InvalidChecksumExceptionMapperTest {

    private InvalidChecksumExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new InvalidChecksumExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final InvalidChecksumException input = new InvalidChecksumException("x didn't match y");
        try (final Response actual = testObj.toResponse(input)) {
            assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
        }
    }
}
