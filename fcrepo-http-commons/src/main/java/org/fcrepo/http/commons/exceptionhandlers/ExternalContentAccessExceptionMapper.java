/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;


import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.ExternalContentAccessException;

import org.slf4j.Logger;

/**
 * ExternalContentException mapper
 *
 * @author whikloj
 * @since 2018-07-11
 */
@Provider
public class ExternalContentAccessExceptionMapper
    implements ExceptionMapper<ExternalContentAccessException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(ExternalContentAccessExceptionMapper.class);

    @Override
    public Response toResponse(final ExternalContentAccessException exception) {
        LOGGER.warn("Failed to read external content", exception);
        return status(BAD_GATEWAY).entity(exception.getMessage()).type(TEXT_PLAIN_WITH_CHARSET)
            .build();
    }

}
