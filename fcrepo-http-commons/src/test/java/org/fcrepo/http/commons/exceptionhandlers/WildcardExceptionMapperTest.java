/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

/**
 * <p>WildcardExceptionMapperTest class.</p>
 *
 * @author awoods
 */
public class WildcardExceptionMapperTest {

    private WildcardExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new WildcardExceptionMapper();
    }

    @Test
    public void testToResponse() {
        testObj.setShowStackTrace(true);
        final Exception input = new Exception();
        Response actual = testObj.toResponse(input);
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), actual.getStatus());
        assertNotNull(actual.getEntity());
        testObj.showStackTrace = false;
        actual = testObj.toResponse(input);
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), actual.getStatus());
        assertNull(actual.getEntity());
    }
}
