/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.status;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.MultipleConstraintViolationException;
import org.fcrepo.kernel.api.exception.RelaxableServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;

import org.slf4j.Logger;

/**
 * Mapper to display all the various constrainedby links and messages.
 * @author whikloj
 */
@Provider
public class MultipleConstraintViolationExceptionMapper extends
        ConstraintExceptionMapper<MultipleConstraintViolationException> implements ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(MultipleConstraintViolationExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final MultipleConstraintViolationException e) {
        debugException(this, e, LOGGER);

        final String msg = e.getMessage();
        final Response.ResponseBuilder response = status(CONFLICT).entity(msg).type(TEXT_PLAIN_WITH_CHARSET);
        final Link[] constraintLinks = e.getExceptionTypes().stream().map(ConstraintViolationException::getClass)
                .distinct().map(c -> {
                    // Avoid building a link with the relaxable sub-class which would require another constraint RDF
                    // file.
                    if (c.equals(RelaxableServerManagedPropertyException.class)) {
                        return ServerManagedPropertyException.class;
                    }
                    return c;
                }).map(c -> buildConstraintLink(c,
                        context,
                        uriInfo))
                .toArray(Link[]::new);
        return response.links(constraintLinks).build();
    }

}
