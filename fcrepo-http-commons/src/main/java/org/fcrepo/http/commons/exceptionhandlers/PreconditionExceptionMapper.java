/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.PreconditionException;

import org.slf4j.Logger;

/**
 * Maps PreconditionException to an appropriate http response.
 * @author Daniel Bernstein
 * @since Jun 22, 2017
 */
@Provider
public class PreconditionExceptionMapper implements
        ExceptionMapper<PreconditionException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(PreconditionExceptionMapper.class);

    @Override
    public Response toResponse(final PreconditionException e) {
        debugException(this, e, LOGGER);
        return status(e.getHttpStatus()).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
