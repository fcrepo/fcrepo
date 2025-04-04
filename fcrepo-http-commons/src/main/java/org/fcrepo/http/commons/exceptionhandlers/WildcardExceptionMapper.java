/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Catch all the exceptions!
 *
 * @author lsitu
 * @author awoods
 * @author cbeer
 * @author fasseg
 */
@Provider
public class WildcardExceptionMapper implements
        ExceptionMapper<Exception>, ExceptionDebugLogging {

    Boolean showStackTrace = false;

    private static final Logger LOGGER =
        getLogger(WildcardExceptionMapper.class);

    @Override
    public Response toResponse(final Exception e) {

        LOGGER.warn("Unmapped exception", e);
        return serverError().entity(
                showStackTrace ? getStackTraceAsString(e) : null).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

    /**
     * Set whether the full stack trace should be returned as part of the
     * error response. This may be a bad idea if the stack trace is exposed
     * to the public.
     * @param showStackTrace the boolean value of showing stack trace
     */
    public void setShowStackTrace(final Boolean showStackTrace) {
        this.showStackTrace = showStackTrace;
    }
}
