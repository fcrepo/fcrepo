/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.api.exception.InvalidMementoPathException;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
/**
 * @author lsitu
 * @since 2018-08-10
 */
@Provider
public class InvalidMementoPathExceptionMapper
        extends ConstraintExceptionMapper<InvalidMementoPathException>
    implements ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(InvalidMementoPathExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final InvalidMementoPathException e) {
        debugException(this, e, LOGGER);
        final Link link = buildConstraintLink(e, context, uriInfo);
        final String msg = e.getMessage();
        return status(BAD_REQUEST).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
