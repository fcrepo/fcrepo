/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;

import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author whikloj
 * @since 10/1/14
 */
@Provider
public class ServerManagedPropertyExceptionMapper extends ConstraintExceptionMapper<ServerManagedPropertyException>
    implements ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(ServerManagedPropertyExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final ServerManagedPropertyException e) {
        debugException(this, e, LOGGER);
        final Link link = buildConstraintLink(e, context, uriInfo);
        final String msg = e.getMessage();
        return status(CONFLICT).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
