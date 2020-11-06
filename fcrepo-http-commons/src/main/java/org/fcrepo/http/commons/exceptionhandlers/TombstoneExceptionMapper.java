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

import org.fcrepo.kernel.api.exception.TombstoneException;

import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/16/14
 */
@Provider
public class TombstoneExceptionMapper implements
        ExceptionMapper<TombstoneException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(TombstoneExceptionMapper.class);

    @Override
    public Response toResponse(final TombstoneException e) {
        LOGGER.debug(e.getMessage());
        final Response.ResponseBuilder response = status(GONE)
                .entity(e.getMessage());

        if (e.getURI() != null) {
            response.link(e.getURI(), "hasTombstone");
        }

        return response.type(TEXT_PLAIN_TYPE).build();
    }
}
