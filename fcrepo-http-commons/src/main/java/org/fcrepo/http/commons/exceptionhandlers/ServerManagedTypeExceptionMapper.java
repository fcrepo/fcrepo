/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.ServerManagedTypeException;

import org.slf4j.Logger;

/**
 * @author whikloj
 * @since 2015-06-02
 */
@Provider
public class ServerManagedTypeExceptionMapper extends ConstraintExceptionMapper<ServerManagedTypeException>
    implements ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(ServerManagedTypeExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final ServerManagedTypeException e) {
        debugException(this, e, LOGGER);
        final Link link = buildConstraintLink(e, context, uriInfo);
        final String msg = e.getMessage();
        return status(CONFLICT).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
