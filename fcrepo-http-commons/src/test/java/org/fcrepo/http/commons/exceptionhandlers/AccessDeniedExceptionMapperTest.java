/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.fcrepo.kernel.api.exception.AccessDeniedException;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>AccessDeniedExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
public class AccessDeniedExceptionMapperTest {

    private AccessDeniedExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new AccessDeniedExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final AccessDeniedException input = new AccessDeniedException("Access Denied",
                                                new Exception("nested exception"));
        final Response actual = testObj.toResponse(input);
        assertEquals(FORBIDDEN.getStatusCode(), actual.getStatus());
    }
}
