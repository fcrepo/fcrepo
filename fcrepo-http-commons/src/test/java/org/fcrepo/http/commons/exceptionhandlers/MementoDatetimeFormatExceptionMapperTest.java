/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.MementoDatetimeFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>MementoDatetimeFormatExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class MementoDatetimeFormatExceptionMapperTest {

    private MementoDatetimeFormatExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new MementoDatetimeFormatExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final MementoDatetimeFormatException input = new MementoDatetimeFormatException(
                                                            "Memento Format Exception",
                                                            new Exception("nested exception"));
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
