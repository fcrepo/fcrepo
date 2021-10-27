/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.slf4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.fromResponse;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle Jersey WebApplicationException
 *
 * @author lsitu
 */
@Provider
public class WebApplicationExceptionMapper implements
        ExceptionMapper<WebApplicationException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(WebApplicationExceptionMapper.class);

    @Override
    public Response toResponse(final WebApplicationException e) {
        LOGGER.warn("Web application error", e);
        final String msg = null == e.getCause() ? e.getMessage() : e.getCause().getMessage();
        // 204, 205, 304 MUST NOT contain an entity body - RFC2616
        switch (e.getResponse().getStatus()) {
            case 204:
            case 205:
            case 304:
                return fromResponse(e.getResponse()).entity(null).build();
            default:
                return fromResponse(e.getResponse()).entity(msg).type(TEXT_PLAIN_WITH_CHARSET).build();
        }
    }
}
