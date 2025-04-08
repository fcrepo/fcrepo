/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import java.net.URI;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.exception.CannotCreateResourceException;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.getServletContextImpl;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private UriInfo mockInfo;

    private ServletContext mockContext;

    @BeforeEach
    public void setUp() {
        this.mockInfo = getUriInfoImpl();
        this.mockContext = getServletContextImpl();
    }

    @Test
    public void testBuildConstraintLink() {
        final URI testLink = URI.create("http://localhost/fcrepo/static/constraints/CannotCreateResourceException.rdf");

        final CannotCreateResourceException ex = new CannotCreateResourceException("This is an error.");

        final Link link = CannotCreateResourceExceptionMapper.buildConstraintLink(ex, mockContext, mockInfo);

        assertEquals(link.getRel(), CONSTRAINED_BY.getURI());
        assertEquals(link.getUri(), testLink);
    }
}
