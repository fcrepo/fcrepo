/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Translate InvalidChecksumException errors into reasonable
 *  HTTP error codes
 *
 * @author awoods
 * @author ajs6f
 * @author cbeer
 */
@Provider
public class InvalidChecksumExceptionMapper implements
        ExceptionMapper<InvalidChecksumException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(InvalidChecksumExceptionMapper.class);

    @Override
    public Response toResponse(final InvalidChecksumException e) {
        LOGGER.debug("Invalid checksum", e);

        return status(CONFLICT).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
