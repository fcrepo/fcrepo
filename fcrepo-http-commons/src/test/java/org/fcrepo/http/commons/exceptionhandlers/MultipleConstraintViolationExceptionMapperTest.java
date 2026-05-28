/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.MultipleConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * MultipleConstraintViolationExceptionTest
 *
 * @author dan.field@lyrasis.org
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MultipleConstraintViolationExceptionMapperTest {

    private MultipleConstraintViolationExceptionMapper testObj;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private ServletContext context;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        testObj = new MultipleConstraintViolationExceptionMapper();
        setField(testObj, "context", context);
        setField(testObj, "uriInfo", uriInfo);
        when(context.getContextPath()).thenReturn("/fcrepo");
        when(uriInfo.getBaseUri()).thenReturn(new URI("http://localhost/fcrepo/static"));
    }

    @Test
    public void testBuildConstraintLink() {
        final URI testLink = URI.create(
                "http://localhost/fcrepo/static/constraints/MultipleConstraintViolationException.rdf");

        final List<ConstraintViolationException> constraints = new ArrayList<>();
        final ConstraintViolationException firstException = new ConstraintViolationException("First Exception");
        final ConstraintViolationException secondException = new ConstraintViolationException("Second Exception");
        constraints.add(firstException);
        constraints.add(secondException);
        final MultipleConstraintViolationException multipleConstraintsException =
                new MultipleConstraintViolationException(constraints);
        final Link link = MultipleConstraintViolationExceptionMapper.buildConstraintLink(
                multipleConstraintsException, context, uriInfo);

        assertEquals(link.getRel(), CONSTRAINED_BY.getURI());
        assertEquals(link.getUri(), testLink);
    }

    @Test
    public void testToResponse() {
        final List<ConstraintViolationException> constraints = new ArrayList<>();
        final ConstraintViolationException firstException = new ConstraintViolationException("First Exception");
        final ConstraintViolationException secondException = new ConstraintViolationException("Second Exception");
        constraints.add(firstException);
        constraints.add(secondException);
        final MultipleConstraintViolationException multipleConstraintsException =
                new MultipleConstraintViolationException(constraints);
        final Response actual = testObj.toResponse(multipleConstraintsException);

        assertEquals(actual.getStatus(), CONFLICT.getStatusCode() );
    }
}
