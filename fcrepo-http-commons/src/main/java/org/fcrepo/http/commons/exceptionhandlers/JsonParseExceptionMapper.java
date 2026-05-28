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

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * If an injected JSON resource fails to parse, return an HTTP 400 Bad Request.
 *
 * @author Esme Cowles
 * @since 2014-05-21
 */
@Provider
public class JsonParseExceptionMapper implements
        ExceptionMapper<JsonParseException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(final JsonParseException e) {
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
