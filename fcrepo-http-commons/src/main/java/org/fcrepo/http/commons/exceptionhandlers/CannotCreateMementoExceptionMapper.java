/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.CannotCreateMementoException;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A mapper for memento creation exceptions
 *
 * @author dbernstein
 */
@Provider
public class CannotCreateMementoExceptionMapper extends ConstraintExceptionMapper<CannotCreateMementoException>
        implements ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(CannotCreateMementoExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final CannotCreateMementoException e) {
        debugException(this, e, LOGGER);
        final Link link = buildConstraintLink(e, context, uriInfo);
        final String msg = e.getMessage();
        return status(BAD_REQUEST).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
