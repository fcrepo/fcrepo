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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.slf4j.Logger;

/**
 * Translate UnsupportedAlgorithmException errors into reasonable
 * HTTP error codes
 *
 * @author harring
 * @since 2017-09-12
 */
@Provider
public class UnsupportedAlgorithmExceptionMapper implements
        ExceptionMapper<UnsupportedAlgorithmException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(UnsupportedAlgorithmExceptionMapper.class);

    @Override
    public Response toResponse(final UnsupportedAlgorithmException e) {

        debugException(this, e, LOGGER);

        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
