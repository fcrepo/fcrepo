/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;

import org.fcrepo.kernel.api.exception.IncorrectTripleSubjectException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * @author ajs6f
 * @since 2015-07-15
 */
@Provider
public class IncorrectTripleSubjectExceptionMapper extends ConstraintExceptionMapper<IncorrectTripleSubjectException>
    implements ExceptionDebugLogging {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IncorrectTripleSubjectExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final IncorrectTripleSubjectException e) {
        debugException(this, e, LOGGER);
        final Link link = buildConstraintLink(e, context, uriInfo);
        final String msg = e.getMessage();
        return status(FORBIDDEN).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
