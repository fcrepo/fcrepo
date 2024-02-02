/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import java.net.URI;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.exception.ConstraintViolationException;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.getServletContextImpl;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.junit.Assert.assertEquals;

import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * ConstraintViolationExceptionMapperTest
 *
 * @author whikloj
 * @since 2015-06-22
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConstraintViolationExceptionMapperTest {

    private UriInfo mockInfo;

    private ServletContext mockContext;

    @Before
    public void setUp() {
        this.mockInfo = getUriInfoImpl();
        this.mockContext = getServletContextImpl();
    }

    @Test
    public void testBuildConstraintLink() {
        final URI testLink = URI.create("http://localhost/fcrepo/static/constraints/ConstraintViolationException.rdf");

        final ConstraintViolationException ex = new ConstraintViolationException("This is an error.");

        final Link link = ConstraintViolationExceptionMapper.buildConstraintLink(ex, mockContext, mockInfo);

        assertEquals(link.getRel(), CONSTRAINED_BY.getURI());
        assertEquals(link.getUri(), testLink);
    }
}
