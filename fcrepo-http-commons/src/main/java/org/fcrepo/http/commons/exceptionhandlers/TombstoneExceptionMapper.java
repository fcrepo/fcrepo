/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.TombstoneException;
import org.slf4j.Logger;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static jakarta.ws.rs.core.Response.Status.GONE;
import static jakarta.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/16/14
 */
@Provider
public class TombstoneExceptionMapper implements
        ExceptionMapper<TombstoneException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(TombstoneExceptionMapper.class);

    @Override
    public Response toResponse(final TombstoneException e) {
        LOGGER.debug(e.getMessage());
        final Response.ResponseBuilder response = status(GONE)
                .entity(e.getMessage());

        if (e.getTombstoneURI() != null) {
            response.link(e.getTombstoneURI(), "hasTombstone");
        }
        if (e.getTimemapUri() != null) {
            response.header(LINK, Link.fromUri(e.getTimemapUri()).rel("timemap").build().toString());
        }

        return response.type(TEXT_PLAIN_TYPE).build();
    }
}
