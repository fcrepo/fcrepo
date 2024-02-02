/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>WildcardExceptionMapperTest class.</p>
 *
 * @author awoods
 */
public class WildcardExceptionMapperTest {

    private WildcardExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new WildcardExceptionMapper();
    }

    @Test
    public void testToResponse() {
        testObj.setShowStackTrace(true);
        final Exception input = new Exception();
        Response actual = testObj.toResponse(input);
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), actual.getStatus());
        assertTrue(actual.getEntity() != null);
        testObj.showStackTrace = false;
        actual = testObj.toResponse(input);
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), actual.getStatus());
        assertNull(actual.getEntity());
    }
}
