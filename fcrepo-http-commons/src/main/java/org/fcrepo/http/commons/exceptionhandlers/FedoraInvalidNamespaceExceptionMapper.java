/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.status;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.FedoraInvalidNamespaceException;

import org.slf4j.Logger;

/**
 * For invalid namespace exceptions on CRUD actions for nodes/datastreams
 *
 * @author whikloj
 * @since September 12, 2014
 */
@Provider
public class FedoraInvalidNamespaceExceptionMapper implements
        ExceptionMapper<FedoraInvalidNamespaceException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(FedoraInvalidNamespaceExceptionMapper.class);

    @Override
    public Response toResponse(final FedoraInvalidNamespaceException e) {
        LOGGER.error("NamespaceExceptionMapper caught an exception: {}", e.getMessage());
        debugException(this, e, LOGGER);
        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
