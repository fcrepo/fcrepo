/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>JsonParseExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class JsonParseExceptionMapperTest {

    private JsonParseExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new JsonParseExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final JsonParseException input = new JsonParseException("Json Parse Exception");
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
