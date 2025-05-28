/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.net.URISyntaxException;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * ServerManagedPropertyExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ServerManagedPropertyExceptionMapperTest {

    private ServerManagedPropertyExceptionMapper testObj;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private ServletContext context;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        testObj = new ServerManagedPropertyExceptionMapper();
        setField(testObj, "context", context);
        setField(testObj, "uriInfo", uriInfo);
        when(context.getContextPath()).thenReturn("/");
        when(uriInfo.getBaseUri()).thenReturn(new URI("/"));
    }

    @Test
    public void testToResponse() {
        final ServerManagedPropertyException input = new ServerManagedPropertyException("Managed Property Error");
        final Response actual = testObj.toResponse(input);
        assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
    }

}
