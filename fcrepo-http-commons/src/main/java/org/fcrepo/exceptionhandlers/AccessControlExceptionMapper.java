
package org.fcrepo.exceptionhandlers;

import org.slf4j.Logger;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.security.AccessControlException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AccessControlExceptionMapper implements
        ExceptionMapper<AccessControlException> {

    private final Logger LOGGER = getLogger(AccessControlExceptionMapper.class);

    @Override
    public Response toResponse(final AccessControlException arg0) {
        LOGGER.warn("Got access control exception: {}", arg0.getMessage());
        return status(FORBIDDEN).build();
    }

}
