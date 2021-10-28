/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The class translates {@link org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException}s to its proper
 * response code.
 *
 * @author awoods
 * @since July 14, 2015.
 */
@Provider
public class InvalidResourceIdentifierExceptionMapper implements
        ExceptionMapper<InvalidResourceIdentifierException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(InvalidResourceIdentifierExceptionMapper.class);

    @Override
    public Response toResponse(final InvalidResourceIdentifierException e) {
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

    public Response toResponse(final String msg, final InvalidResourceIdentifierException e) {
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(msg).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
