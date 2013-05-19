
package org.fcrepo.exceptionhandlers;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.PathNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

@Provider
public class PathNotFoundExceptionMapper implements
        ExceptionMapper<PathNotFoundException> {

    private static final Logger logger =
            getLogger(PathNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(final PathNotFoundException e) {
        logger.error("PathNotFoundExceptionMapper intercepted exception: \n", e);
        return status(NOT_FOUND).build();
    }

}
