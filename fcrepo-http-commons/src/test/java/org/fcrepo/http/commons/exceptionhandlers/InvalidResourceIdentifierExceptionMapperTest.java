/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>InvalidMementoPathExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class InvalidResourceIdentifierExceptionMapperTest {

    private InvalidResourceIdentifierExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new InvalidResourceIdentifierExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final InvalidResourceIdentifierException input = new InvalidResourceIdentifierException("Invalid Resource",
                                                new Exception("nested exception"));
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
