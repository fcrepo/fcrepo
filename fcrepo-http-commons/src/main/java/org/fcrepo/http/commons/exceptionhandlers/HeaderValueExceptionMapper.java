/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.glassfish.jersey.message.internal.HeaderValueException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static javax.ws.rs.core.Response.status;

/**
 * If a client-provided header value fails to parse, return an HTTP 400 Bad Request.
 *
 * @author awoods
 * @since 2015-08-06
 */
@Provider
public class HeaderValueExceptionMapper implements
        ExceptionMapper<HeaderValueException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(HeaderValueExceptionMapper.class);

    @Override
    public Response toResponse(final HeaderValueException e) {
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(e.getMessage() + " ...should value be quoted?").type(TEXT_PLAIN_WITH_CHARSET)
                .build();
    }
}
