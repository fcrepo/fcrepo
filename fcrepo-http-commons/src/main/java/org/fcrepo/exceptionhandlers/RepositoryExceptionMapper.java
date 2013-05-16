package org.fcrepo.exceptionhandlers;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static org.slf4j.LoggerFactory.getLogger;

@Provider
public class RepositoryExceptionMapper implements ExceptionMapper<RepositoryException> {

    private final Logger LOGGER = getLogger(RepositoryExceptionMapper.class);

    Boolean showStackTrace = true;

    @Override
    public Response toResponse(RepositoryException e) {

        LOGGER.warn("Caught repository exception: {}", e);

        return serverError().entity(showStackTrace ? getStackTraceAsString(e) : null).build();
    }
}
