/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.glassfish.jersey.server.ParamException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * {@link org.fcrepo.http.commons.exceptionhandlers.ParamExceptionMapper}
 *
 * @author awoods
 * @since 2015-01-20
 */
public class ParamExceptionMapperTest {

    private ParamExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new ParamExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ParamException input = new ParamException.HeaderParamException(new RuntimeException("canned-exception"),
                                                                             "test-header",
                                                                             null);
        final Response actual = testObj.toResponse(input);
        assertEquals(input.getResponse().getStatus(), actual.getStatus());
        assertNotNull(actual.getEntity());
    }
}
