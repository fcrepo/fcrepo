/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import org.fcrepo.kernel.api.exception.SessionMissingException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Catch all the exceptions!
 *
 * @author lsitu
 * @author awoods
 * @author cbeer
 * @author fasseg
 */
@Provider
public class WildcardExceptionMapper implements
        ExceptionMapper<Exception>, ExceptionDebugLogging {

    Boolean showStackTrace = false;

    private static final Logger LOGGER =
        getLogger(WildcardExceptionMapper.class);

    @Override
    public Response toResponse(final Exception e) {
        if (e.getCause() instanceof SessionMissingException) {
            return new SessionMissingExceptionMapper()
                    .toResponse((SessionMissingException) e.getCause());
        }

        LOGGER.warn("Unmapped exception", e);
        return serverError().entity(
                showStackTrace ? getStackTraceAsString(e) : null).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

    /**
     * Set whether the full stack trace should be returned as part of the
     * error response. This may be a bad idea if the stack trace is exposed
     * to the public.
     * @param showStackTrace the boolean value of showing stack trace
     */
    public void setShowStackTrace(final Boolean showStackTrace) {
        this.showStackTrace = showStackTrace;
    }
}
