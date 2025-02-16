/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.core.Response;

import org.fcrepo.kernel.api.exception.PathNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * PathNotFoundExceptionMapperTest class.
 *
 * @author robyj
 */
public class PathNotFoundExceptionMapperTest {

    private PathNotFoundExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new PathNotFoundExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final PathNotFoundException input = new PathNotFoundException("xyz");
        final Response actual = testObj.toResponse(input);
        assertEquals(NOT_FOUND.getStatusCode(), actual.getStatus());
        assertEquals(actual.getEntity(), "Error: xyz");
    }
}
