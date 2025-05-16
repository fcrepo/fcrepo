/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.slf4j.LoggerFactory.getLogger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.slf4j.Logger;


/**
 * Catch PathNotFoundRuntimeException(s)
 *
 * @author whikloj
 */
@Provider
public class PathNotFoundRuntimeExceptionMapper implements
        ExceptionMapper<PathNotFoundRuntimeException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(PathNotFoundRuntimeExceptionMapper.class);

    @Override
    public Response toResponse(final PathNotFoundRuntimeException e) {
        debugException(this, e, LOGGER);
        return Response.status(Response.Status.NOT_FOUND).
                entity("Error: " + e.getMessage()).build();
    }
}

