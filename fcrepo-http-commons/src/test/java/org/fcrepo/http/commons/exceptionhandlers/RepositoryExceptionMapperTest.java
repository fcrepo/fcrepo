/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fcrepo.kernel.api.exception.RepositoryException;

import jakarta.ws.rs.core.Response;

/**
 * @author escowles
 * @since 2014-04-19
 */
public class RepositoryExceptionMapperTest {

    private RepositoryExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new RepositoryExceptionMapper();
    }

    @Test
    public void testInvalidNamespace() {
        final RepositoryException input = new RepositoryException("Error converting \"abc:123\" from String to a Name");
        try (final Response actual = testObj.toResponse(input)) {
            assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
            assertEquals(actual.getEntity(), input.getMessage());
        }
    }

    @Test
    public void testToResponse() {
        final RepositoryException input = new RepositoryException("An error occurred");
        try (final Response actual = testObj.toResponse(input)) {
            assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), actual.getStatus());
            assertNotNull(actual.getEntity());
        }
    }
}
