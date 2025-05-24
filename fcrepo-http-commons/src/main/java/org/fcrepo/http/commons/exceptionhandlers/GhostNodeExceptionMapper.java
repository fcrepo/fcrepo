/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.status;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.GhostNodeException;

/**
 * Map an GhostNodeException to a response.
 * @author whikloj
 */
@Provider
public class GhostNodeExceptionMapper implements ExceptionMapper<GhostNodeException> {

    @Override
    public Response toResponse(final GhostNodeException e) {
        final String msg = e.getMessage();
        return status(CONFLICT).entity(msg).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
