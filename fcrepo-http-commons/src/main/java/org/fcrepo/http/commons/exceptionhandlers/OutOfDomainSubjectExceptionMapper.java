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

import org.fcrepo.kernel.api.exception.OutOfDomainSubjectException;

import org.slf4j.Logger;

import javax.servlet.ServletContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

/**
 * @author whikloj
 * @since 2015-05-29
 */
@Provider
public class OutOfDomainSubjectExceptionMapper extends ConstraintExceptionMapper<OutOfDomainSubjectException>
    implements ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(OutOfDomainSubjectExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final OutOfDomainSubjectException e) {

        debugException(this, e, LOGGER);
        final Link link = buildConstraintLink(e, context, uriInfo);
        final String msg = e.getMessage();
        return status(BAD_REQUEST).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
