/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.SessionMissingException;

import org.slf4j.Logger;

/**
 * If a session is requested that has been closed (or never existed), just
 * return an HTTP 410 Gone.
 *
 * @author awoods
 */
@Provider
public class SessionMissingExceptionMapper implements
        ExceptionMapper<SessionMissingException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(SessionMissingExceptionMapper.class);


    @Override
    public Response toResponse(final SessionMissingException e) {
        debugException(this, e, LOGGER);
        return status(GONE).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
