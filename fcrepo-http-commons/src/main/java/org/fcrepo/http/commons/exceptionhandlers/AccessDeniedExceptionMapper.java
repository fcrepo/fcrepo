/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fasseg
 */
@Provider
public class AccessDeniedExceptionMapper implements
        ExceptionMapper<AccessDeniedException>, ExceptionDebugLogging {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccessDeniedExceptionMapper.class);

    /*
     * (non-Javadoc)
     * @see jakarta.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
     */
    @Override
    public Response toResponse(final AccessDeniedException e) {
        debugException(this, e, LOGGER);
        return status(FORBIDDEN).build();
    }
}
