/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.exception.CannotCreateResourceException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.fcrepo.http.commons.test.util.TestHelpers.getServletContextImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * CannotCreateResourceExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CannotCreateResourceExceptionMapperTest {

    @Mock
    private UriInfo mockInfo;

    @Mock
    private ServletContext mockContext;

    private CannotCreateResourceExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new CannotCreateResourceExceptionMapper();
        this.mockInfo = getUriInfoImpl();
        this.mockContext = getServletContextImpl();
        setField(testObj, "uriInfo", mockInfo);
        setField(testObj, "context", mockContext);
    }

    @Test
    public void testToResponse() {
            final CannotCreateResourceException input = new CannotCreateResourceException("Exception Message");
            final Response actual = testObj.toResponse(input);
            assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
