/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.glassfish.jersey.server.ParamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

/**
 * {@link org.fcrepo.http.commons.exceptionhandlers.ParamExceptionMapper}
 *
 * @author awoods
 * @since 2015-01-20
 */
public class ParamExceptionMapperTest {

    private ParamExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new ParamExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final ParamException input = new ParamException.HeaderParamException(new RuntimeException("canned-exception"),
                                                                             "test-header",
                                                                             null);
        try (final Response actual = testObj.toResponse(input)) {
            assertEquals(input.getResponse().getStatus(), actual.getStatus());
            assertNotNull(actual.getEntity());
        }
    }
}
