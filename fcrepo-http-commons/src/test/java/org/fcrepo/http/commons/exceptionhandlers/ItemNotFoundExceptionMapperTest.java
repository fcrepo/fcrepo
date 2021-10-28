/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.junit.Before;
import org.junit.Test;


/**
 * ItemNotFoundExceptionMapperTest class.
 *
 * @author pwinckles
 */
public class ItemNotFoundExceptionMapperTest {

    private ItemNotFoundExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new ItemNotFoundExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ItemNotFoundException input = new ItemNotFoundException("xyz");
        final Response actual = testObj.toResponse(input);
        assertEquals(NOT_FOUND.getStatusCode(), actual.getStatus());
        assertEquals(actual.getEntity(), "Error: xyz");
    }
}
