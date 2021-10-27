/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.InterruptedRuntimeException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;

/**
 * If an HTTP request's processing is interrupted, return an HTTP 503 Service Unavailable.
 *
 * @author Mike Durbin
 */
@Provider
public class InterruptedExceptionMapper implements
        ExceptionMapper<InterruptedRuntimeException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(InterruptedExceptionMapper.class);

    @Override
    public Response toResponse(final InterruptedRuntimeException e) {
        LOGGER.warn("Service interrupted", e);
        return status(SERVICE_UNAVAILABLE).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
