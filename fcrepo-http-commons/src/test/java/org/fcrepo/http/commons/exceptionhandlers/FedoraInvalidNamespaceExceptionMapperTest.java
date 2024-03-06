/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;

import jakarta.ws.rs.core.Response;

/**
 * <p>
 * FedoraInvalidNamespaceExceptionMapperTest class.
 * </p>
 *
 * @author Daniel Bernstein
 * @since January 19, 2017
 */
public class FedoraInvalidNamespaceExceptionMapperTest {

    private FedoraInvalidNamespaceExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new FedoraInvalidNamespaceExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final FedoraInvalidNamespaceException input = new FedoraInvalidNamespaceException(
                "Invalid namespace", null);
        try (final Response actual = testObj.toResponse(input)) {
            assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
            assertEquals(TEXT_PLAIN_WITH_CHARSET, actual.getHeaderString(CONTENT_TYPE));
        }
    }
}
