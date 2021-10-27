/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
     * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
     */
    @Override
    public Response toResponse(final AccessDeniedException e) {
        debugException(this, e, LOGGER);
        return status(FORBIDDEN).build();
    }
}
