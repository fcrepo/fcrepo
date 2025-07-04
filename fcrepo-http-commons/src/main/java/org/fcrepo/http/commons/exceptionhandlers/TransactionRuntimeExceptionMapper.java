/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.TransactionRuntimeException;
import org.slf4j.Logger;

/**
 * Mapper for transaction exceptions
 *
 * @author bbpennel
 */
@Provider
public class TransactionRuntimeExceptionMapper
        implements ExceptionMapper<TransactionRuntimeException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(TransactionRuntimeExceptionMapper.class);

    @Override
    public Response toResponse(final TransactionRuntimeException exception) {
        debugException(this, exception, LOGGER);

        return status(Status.CONFLICT)
                .entity(exception.getMessage())
                .type(TEXT_PLAIN_WITH_CHARSET)
                .build();
    }

}
