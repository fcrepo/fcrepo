/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.junit.Assert.assertEquals;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;

import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void setUp() {
        testObj = new FedoraInvalidNamespaceExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final FedoraInvalidNamespaceException input = new FedoraInvalidNamespaceException(
                "Invalid namespace", null);
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
        assertEquals(TEXT_PLAIN_WITH_CHARSET, actual.getHeaderString(CONTENT_TYPE));

    }
}
