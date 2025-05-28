/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.slf4j.Logger;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;

/**
 * For generic BadRequestExceptions.
 *
 * @author md5wz
 * @since November 18, 2014
 */
@Provider
public class BadRequestExceptionMapper implements
        ExceptionMapper<BadRequestException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(BadRequestExceptionMapper.class);

    @Override
    public Response toResponse(final BadRequestException e) {
        LOGGER.error("BadRequestExceptionMapper caught an exception: {}", e.getMessage());
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}