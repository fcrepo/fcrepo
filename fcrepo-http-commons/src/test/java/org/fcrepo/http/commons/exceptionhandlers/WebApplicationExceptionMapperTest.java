/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

/**
 * <p>WebApplicationExceptionMapperTest class.</p>
 *
 * @author lsitu
 * @author awoods
 */
public class WebApplicationExceptionMapperTest {

    private WebApplicationExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new WebApplicationExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final WebApplicationException input = new WebApplicationException();
        final Response actual = testObj.toResponse(input);
        assertEquals(input.getResponse().getStatus(), actual.getStatus());
    }

    /**
     * Insures that the WebApplicationExceptionMapper does not provide an entity body to 204, 205, or 304 responses.
     * Entity bodies on other responses are mapped appropriately.
     */
    @Test
    public void testNoEntityBody() {
        Stream.of(204, 205, 304).forEach(status -> {
                    final WebApplicationException input = new WebApplicationException("Error message", status);
                    final Response actual = testObj.toResponse(input);
                    assertNull(actual.getEntity(),
                            "Responses with a " + status + " status code MUST NOT carry an entity body.");
                }
        );

        final WebApplicationException input = new WebApplicationException("Error message", 500);
        final Response actual = testObj.toResponse(input);
        assertEquals("Error message", actual.getEntity());
    }
}
