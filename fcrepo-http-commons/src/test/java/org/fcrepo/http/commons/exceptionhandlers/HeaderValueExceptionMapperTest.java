/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.glassfish.jersey.message.internal.HeaderValueException;
import org.glassfish.jersey.message.internal.HeaderValueException.Context;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>BadRequestExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class HeaderValueExceptionMapperTest {

    private HeaderValueExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new HeaderValueExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final HeaderValueException input = new HeaderValueException("Access Denied",
                                            new Exception("nested exception"),
                                            Context.OUTBOUND);
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
