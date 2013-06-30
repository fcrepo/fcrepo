/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.exceptionhandlers;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.exception.TransactionMissingException;
import org.slf4j.Logger;

/**
 * Catch all the exceptions!
 */
@Provider
public class WildcardExceptionMapper implements ExceptionMapper<Exception> {

    Boolean showStackTrace = true;

    private static final Logger logger =
            getLogger(WildcardExceptionMapper.class);

    @Override
    public Response toResponse(final Exception e) {

        if (WebApplicationException.class.isAssignableFrom(e.getClass())) {
            logger.info(
                    "WebApplicationException intercepted by WildcardExceptionMapper: \n",
                    e);
            return ((WebApplicationException) e).getResponse();
        }

        if (e.getCause() instanceof TransactionMissingException) {
            return new TransactionMissingExceptionMapper()
                    .toResponse((TransactionMissingException) e.getCause());
        }

        logger.error("Exception intercepted by WildcardExceptionMapper: \n", e);
        return serverError().entity(
                showStackTrace ? getStackTraceAsString(e) : null).build();
    }

    public void setShowStackTrace(final Boolean showStackTrace) {
        this.showStackTrace = showStackTrace;
    }
}