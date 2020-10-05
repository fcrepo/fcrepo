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

import org.fcrepo.kernel.api.exception.InsufficientStorageException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Translate InsufficientStorageException errors into HTTP error codes
 *
 * @author Daniel Bernstein
 * @since Oct 7, 2016
 */
@Provider
public class InsufficientStorageExceptionMapper implements
        ExceptionMapper<InsufficientStorageException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(InsufficientStorageException.class);

    public static final int INSUFFICIENT_STORAGE_HTTP_CODE = 507;

    @Override
    public Response toResponse(final InsufficientStorageException e) {
        LOGGER.error("Insufficient storage", e);
        return status(INSUFFICIENT_STORAGE_HTTP_CODE).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }
}
