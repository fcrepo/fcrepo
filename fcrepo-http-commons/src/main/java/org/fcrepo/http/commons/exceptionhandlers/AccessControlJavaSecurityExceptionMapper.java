/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.slf4j.LoggerFactory.getLogger;

import java.security.AccessControlException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * Translate Java Security AccessControlExceptions into HTTP 403 Forbidden errors
 *
 * @author lsitu
 * @author awoods
 * @author gregjan
 */
@Provider
public class AccessControlJavaSecurityExceptionMapper implements
        ExceptionMapper<AccessControlException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(AccessControlJavaSecurityExceptionMapper.class);

    @Override
    public Response toResponse(final AccessControlException e) {
        debugException(this, e, LOGGER);
        return status(FORBIDDEN).build();
    }

}
