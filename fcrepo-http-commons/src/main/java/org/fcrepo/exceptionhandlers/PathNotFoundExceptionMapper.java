
package org.fcrepo.exceptionhandlers;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.jcr.PathNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class PathNotFoundExceptionMapper implements
        ExceptionMapper<PathNotFoundException> {

    @Override
    public Response toResponse(PathNotFoundException arg0) {
        return status(NOT_FOUND).build();
    }

}
