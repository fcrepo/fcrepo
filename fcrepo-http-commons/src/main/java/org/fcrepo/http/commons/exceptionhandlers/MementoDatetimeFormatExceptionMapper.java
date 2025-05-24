/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.MementoDatetimeFormatException;
import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static jakarta.ws.rs.core.Response.status;

/**
 * Handle MementoDatetimeFormatException with HTTP 400 Bad Request.
 *
 * @author lsitu
 * @since 2018-03-12
 */
@Provider
public class MementoDatetimeFormatExceptionMapper implements
        ExceptionMapper<MementoDatetimeFormatException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(MementoDatetimeFormatExceptionMapper.class);

    @Override
    public Response toResponse(final MementoDatetimeFormatException e) {
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET)
                .build();
    }
}
