/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;

import jakarta.ws.rs.core.Response;


/**
 * ItemNotFoundExceptionMapperTest class.
 *
 * @author pwinckles
 */
public class ItemNotFoundExceptionMapperTest {

    private ItemNotFoundExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new ItemNotFoundExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ItemNotFoundException input = new ItemNotFoundException("xyz");
        try (final Response actual = testObj.toResponse(input)) {
            assertEquals(NOT_FOUND.getStatusCode(), actual.getStatus());
            assertEquals(actual.getEntity(), "Error: xyz");
        }
    }
}
