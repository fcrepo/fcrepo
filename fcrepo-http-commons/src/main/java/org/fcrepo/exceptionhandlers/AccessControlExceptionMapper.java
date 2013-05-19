
package org.fcrepo.exceptionhandlers;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.security.AccessControlException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

@Provider
public class AccessControlExceptionMapper implements
        ExceptionMapper<AccessControlException> {

    private static final Logger logger =
            getLogger(AccessControlExceptionMapper.class);

    @Override
    public Response toResponse(final AccessControlException e) {
        logger.error("AccessControlExceptionMapper intercepted exception: \n",
                e);

        return status(FORBIDDEN).build();
    }

}
