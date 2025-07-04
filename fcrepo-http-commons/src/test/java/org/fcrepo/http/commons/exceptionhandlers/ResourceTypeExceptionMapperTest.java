/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fcrepo.kernel.api.exception.ResourceTypeException;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>ResourceTypeExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class ResourceTypeExceptionMapperTest {

    private ResourceTypeExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new ResourceTypeExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ResourceTypeException input = new ResourceTypeException("Bad Request Type");
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
