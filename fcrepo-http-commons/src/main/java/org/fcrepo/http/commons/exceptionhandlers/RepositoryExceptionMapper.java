/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.RepositoryException;
import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.serverError;
import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provide a quasi-useful stacktrace when a generic RepositoryException is caught
 *
 * @author awoods
 */
@Provider
public class RepositoryExceptionMapper implements
                                       ExceptionMapper<RepositoryException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(RepositoryExceptionMapper.class);

    @Override
    public Response toResponse(final RepositoryException e) {
        LOGGER.error("Caught a repository exception", e);

        if (e.getMessage().matches("Error converting \".+\" from String to a Name")) {
            return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
        }

        return serverError().entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
