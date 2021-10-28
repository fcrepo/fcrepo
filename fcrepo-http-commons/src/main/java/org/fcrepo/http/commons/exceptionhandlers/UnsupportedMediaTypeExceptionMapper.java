/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static javax.ws.rs.core.Response.status;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.UnsupportedMediaTypeException;
import org.slf4j.Logger;

/**
 * UnsupportedMediaType mapper
 * @author whikloj
 * @since 6.0.0
 */
@Provider
public class UnsupportedMediaTypeExceptionMapper implements
        ExceptionMapper<UnsupportedMediaTypeException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(UnsupportedMediaTypeExceptionMapper.class);

    @Override
    public Response toResponse(final UnsupportedMediaTypeException e) {
        debugException(this, e, LOGGER);

        return status(UNSUPPORTED_MEDIA_TYPE).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
