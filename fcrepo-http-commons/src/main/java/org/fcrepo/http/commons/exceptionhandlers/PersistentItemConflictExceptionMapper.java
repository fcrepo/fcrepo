/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Maps PersistentItemConflictException to an appropriate http response.
 *
 * @author dbernstein
 * @since 1/29/2020
 */
@Provider
public class PersistentItemConflictExceptionMapper implements
        ExceptionMapper<PersistentItemConflictException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(PersistentItemConflictExceptionMapper.class);

    @Override
    public Response toResponse(final PersistentItemConflictException e) {
        debugException(this, e, LOGGER);
        return status(CONFLICT.getStatusCode()).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
