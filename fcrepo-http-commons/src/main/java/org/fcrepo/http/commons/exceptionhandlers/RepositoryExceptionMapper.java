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

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.RepositoryException;
import org.slf4j.Logger;

/**
 * Provide a quasi-useful stacktrace when a generic RepositoryException is caught
 *
 * @author awoods
 */
@Provider
public class RepositoryExceptionMapper implements
                                       ExceptionMapper<RepositoryException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(RepositoryExceptionMapper.class);

    @Override
    public Response toResponse(final RepositoryException e) {

        LOGGER.error("Caught a repository exception: {}", e.getMessage());
        debugException(this, e, LOGGER);
        if (e.getMessage().matches("Error converting \".+\" from String to a Name")) {
            return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
        }

        return serverError().entity(getStackTraceAsString(e)).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
