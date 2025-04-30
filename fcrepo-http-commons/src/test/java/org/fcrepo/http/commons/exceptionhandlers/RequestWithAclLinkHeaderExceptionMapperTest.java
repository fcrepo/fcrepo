/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.net.URI;
import java.net.URISyntaxException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * <p>RequestWithAclLinkHeaderExceptionMapperTest class.</p>
 *
 * @author dan.field@lyrasis.org
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RequestWithAclLinkHeaderExceptionMapperTest {

    @Mock
    private UriInfo uriInfo;

    @Mock
    private ServletContext context;

    private RequestWithAclLinkHeaderExceptionMapper testObj;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        testObj = new RequestWithAclLinkHeaderExceptionMapper();
        setField(testObj, "context", context);
        setField(testObj, "uriInfo", uriInfo);
        when(context.getContextPath()).thenReturn("/fcrepo");
        when(uriInfo.getBaseUri()).thenReturn(new URI("http://localhost/fcrepo/static"));
    }

    @Test
    public void testToResponse() {
        final RequestWithAclLinkHeaderException input = new RequestWithAclLinkHeaderException("Access Denied");
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
    }
}
