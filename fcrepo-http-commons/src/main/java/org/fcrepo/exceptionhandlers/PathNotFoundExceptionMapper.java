
package org.fcrepo.exceptionhandlers;

import org.slf4j.Logger;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.PathNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class PathNotFoundExceptionMapper implements
        ExceptionMapper<PathNotFoundException> {


    private final Logger LOGGER = getLogger(PathNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(final PathNotFoundException arg0) {
        LOGGER.info("Caught a path-not-found exception: {}", arg0.getMessage());
        return status(NOT_FOUND).build();
    }

}
