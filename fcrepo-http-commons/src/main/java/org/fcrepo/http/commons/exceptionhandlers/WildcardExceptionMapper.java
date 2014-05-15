/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.security.AccessControlException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.exception.TransactionMissingException;
import org.slf4j.Logger;

/**
 * Catch all the exceptions!
 *
 * @author awoods
 * @author cbeer
 * @author fasseg
 */
@Provider
public class WildcardExceptionMapper implements ExceptionMapper<Exception> {

    Boolean showStackTrace = true;

    private static final Logger LOGGER =
        getLogger(WildcardExceptionMapper.class);

    @Override
    public Response toResponse(final Exception e) {

        if (WebApplicationException.class.isAssignableFrom(e.getClass())) {
            LOGGER.debug(
                            "WebApplicationException intercepted by WildcardExceptionMapper: \n",
                            e);
            return ((WebApplicationException) e).getResponse();
        }

        if (java.security.AccessControlException.class.isAssignableFrom(e
                .getClass())) {
            return new AccessControlExceptionMapper()
                    .toResponse((java.security.AccessControlException) e);
        }

        if (javax.jcr.AccessDeniedException.class.isAssignableFrom(e.getClass())) {
            return new AccessDeniedExceptionMapper()
                    .toResponse((javax.jcr.AccessDeniedException) e);
        }
        if (AccessControlException.class.isAssignableFrom(e.getClass())) {
            return new AccessControlExceptionMapper()
                    .toResponse((AccessControlException) e);
        }

        if (e.getCause() instanceof LockException) {
            return new LockExceptionMapper()
                    .toResponse((LockException) e.getCause());
        }

        if (e.getCause() instanceof TransactionMissingException) {
            return new TransactionMissingExceptionMapper()
                    .toResponse((TransactionMissingException) e.getCause());
        }

        if ( e.getCause() instanceof RepositoryException) {
            return new RepositoryExceptionMapper()
                    .toResponse((RepositoryException)e.getCause());
        }

        LOGGER.info("Exception intercepted by WildcardExceptionMapper: \n", e);
        return serverError().entity(
                showStackTrace ? getStackTraceAsString(e) : null).build();
    }

    /**
     * Set whether the full stack trace should be returned as part of the
     * error response. This may be a bad idea if the stack trace is exposed
     * to the public.
     * @param showStackTrace
     */
    public void setShowStackTrace(final Boolean showStackTrace) {
        this.showStackTrace = showStackTrace;
    }
}
