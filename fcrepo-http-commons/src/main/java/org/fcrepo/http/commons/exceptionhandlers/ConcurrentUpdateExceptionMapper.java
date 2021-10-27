/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author pwinckles
 */
@Provider
public class ConcurrentUpdateExceptionMapper implements
        ExceptionMapper<ConcurrentUpdateException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(ConcurrentUpdateExceptionMapper.class);

    @Override
    public Response toResponse(final ConcurrentUpdateException e) {
        debugException(this, e, LOGGER);
        return status(Response.Status.CONFLICT).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}