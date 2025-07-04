/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;

import org.fcrepo.kernel.api.exception.PreconditionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PreconditionExceptionTest class.
 *
 * @author dbernstein
 * @since Jun 22, 2017
 */
public class PreconditionExceptionMapperTest {

    private PreconditionExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new PreconditionExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final String message = "error message";
        final PreconditionException input = new PreconditionException(message, PRECONDITION_FAILED.getStatusCode());
        final Response actual = testObj.toResponse(input);
        assertEquals(PRECONDITION_FAILED.getStatusCode(), actual.getStatus());
        assertEquals(message, actual.getEntity().toString());

    }
}
