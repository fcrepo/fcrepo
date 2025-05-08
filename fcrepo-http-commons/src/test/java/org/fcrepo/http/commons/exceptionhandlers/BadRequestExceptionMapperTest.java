/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>BadRequestExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class BadRequestExceptionMapperTest {

    private BadRequestExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new BadRequestExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final BadRequestException input = new BadRequestException("Access Denied",
                                                new Exception("nested exception"));
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
