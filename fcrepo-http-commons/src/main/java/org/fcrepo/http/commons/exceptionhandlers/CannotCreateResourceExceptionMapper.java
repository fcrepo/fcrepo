/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.api.exception.CannotCreateResourceException;

import org.slf4j.Logger;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
/**
 * @author bseeger
 * @since 2017-04-07
 */
@Provider
public class CannotCreateResourceExceptionMapper extends ConstraintExceptionMapper<CannotCreateResourceException>
    implements ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(CannotCreateResourceExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final CannotCreateResourceException e) {
        debugException(this, e, LOGGER);
        final Link link = buildConstraintLink(e, context, uriInfo);
        final String msg = e.getMessage();
        return status(BAD_REQUEST).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
