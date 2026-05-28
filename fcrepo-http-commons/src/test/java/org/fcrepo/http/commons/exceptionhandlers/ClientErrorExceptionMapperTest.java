/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ClientErrorException;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ClientErrorExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
public class ClientErrorExceptionMapperTest {

    private ClientErrorExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new ClientErrorExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ClientErrorException input = new ClientErrorException("Access Denied", BAD_REQUEST);
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }

}
