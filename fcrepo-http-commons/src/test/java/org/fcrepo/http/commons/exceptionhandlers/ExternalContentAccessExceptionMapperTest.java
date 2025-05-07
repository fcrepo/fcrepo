/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.ExternalContentAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ClientErrorExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
public class ExternalContentAccessExceptionMapperTest {

    private ExternalContentAccessExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new ExternalContentAccessExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ExternalContentAccessException input = new ExternalContentAccessException("Access Denied",
                new Exception("Injected Exception"));
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_GATEWAY.getStatusCode(), actual.getStatus());
    }

}
