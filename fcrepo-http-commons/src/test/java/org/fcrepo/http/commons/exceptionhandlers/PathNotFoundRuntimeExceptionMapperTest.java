/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>PathNotFoundRuntimeExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class PathNotFoundRuntimeExceptionMapperTest {

    private PathNotFoundRuntimeExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new PathNotFoundRuntimeExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final PathNotFoundRuntimeException input = new PathNotFoundRuntimeException("Json Parse Exception");
        final Response actual = testObj.toResponse(input);
        assertEquals(NOT_FOUND.getStatusCode(), actual.getStatus());
    }
}
