
package org.fcrepo.exceptionhandlers;

import org.slf4j.Logger;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WildcardExceptionMapper implements ExceptionMapper<Exception> {


    private final Logger LOGGER = getLogger(WildcardExceptionMapper.class);

    Boolean showStackTrace = true;

    @Override
    public Response toResponse(final Exception e) {
        LOGGER.error("{}", e);

        return serverError().entity(
                showStackTrace ? getStackTraceAsString(e) : null).build();
    }

    public void setShowStackTrace(final Boolean showStackTrace) {
        this.showStackTrace = showStackTrace;
    }
}