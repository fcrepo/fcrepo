/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Translate InsufficientStorageException errors into HTTP error codes
 *
 * @author Daniel Bernstein
 * @since Oct 7, 2016
 */
@Provider
public class InsufficientStorageExceptionMapper implements
        ExceptionMapper<InsufficientStorageException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(InsufficientStorageException.class);

    public static final int INSUFFICIENT_STORAGE_HTTP_CODE = 507;

    @Override
    public Response toResponse(final InsufficientStorageException e) {
        LOGGER.error("Insufficient storage", e);
        return status(INSUFFICIENT_STORAGE_HTTP_CODE).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
