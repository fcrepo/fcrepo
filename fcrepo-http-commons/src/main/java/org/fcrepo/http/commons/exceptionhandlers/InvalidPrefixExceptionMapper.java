/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.InvalidPrefixException;
import org.slf4j.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * For invalid namespace exceptions on CRUD actions for nodes/datastreams
 *
 * @author nianma
 * @since November 2, 2015
 */
@Provider
public class InvalidPrefixExceptionMapper implements
        ExceptionMapper<InvalidPrefixException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(InvalidPrefixExceptionMapper.class);

    @Override
    public Response toResponse(final InvalidPrefixException e) {
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
