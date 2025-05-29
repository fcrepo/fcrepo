/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.ResourceTypeException;

import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static jakarta.ws.rs.core.Response.status;

/**
 * @author cabeer
 * @since 9/15/14
 */
@Provider
public class ResourceTypeExceptionMapper implements
        ExceptionMapper<ResourceTypeException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(ResourceTypeExceptionMapper.class);

    @Override
    public Response toResponse(final ResourceTypeException e) {
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(null).build();
    }
}
