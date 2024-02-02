/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.RepositoryVersionRuntimeException;

import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/15/14
 */
@Provider
public class RepositoryVersionRuntimeExceptionMapper implements
        ExceptionMapper<RepositoryVersionRuntimeException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(RepositoryVersionRuntimeExceptionMapper.class);

    @Override
    public Response toResponse(final RepositoryVersionRuntimeException e) {
        debugException(this, e, LOGGER);
        return status(NOT_FOUND).entity("This resource is not versioned").type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
