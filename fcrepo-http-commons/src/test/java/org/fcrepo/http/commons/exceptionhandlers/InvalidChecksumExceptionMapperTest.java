/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.Assert.assertEquals;

import jakarta.ws.rs.core.Response;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.junit.Before;
import org.junit.Test;

/**
 * <p>InvalidChecksumExceptionMapperTest class.</p>
 *
 * @author awoods
 */
public class InvalidChecksumExceptionMapperTest {

    private InvalidChecksumExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new InvalidChecksumExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final InvalidChecksumException input = new InvalidChecksumException("x didn't match y");
        final Response actual = testObj.toResponse(input);
        assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
    }
}
