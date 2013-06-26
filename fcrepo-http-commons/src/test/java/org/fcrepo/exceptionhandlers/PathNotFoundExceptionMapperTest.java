
package org.fcrepo.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import javax.jcr.PathNotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

public class PathNotFoundExceptionMapperTest {

    private PathNotFoundExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new PathNotFoundExceptionMapper();
    }

    @Test
    public void testToResponse() {
        PathNotFoundException input = new PathNotFoundException();
        Response actual = testObj.toResponse(input);
        assertEquals(NOT_FOUND.getStatusCode(), actual.getStatus());
    }
}
