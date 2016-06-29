/*
 * Copyright 2015 DuraSpace, Inc.
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

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import static javax.ws.rs.core.Response.fromResponse;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author awoods
 * @since 11/20/14
 */
@Provider
public class ClientErrorExceptionMapper implements
        ExceptionMapper<ClientErrorException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(ClientErrorExceptionMapper.class);

    @Override
    public Response toResponse(final ClientErrorException e) {
        debugException(this, e, LOGGER);
        return fromResponse(e.getResponse()).entity(e.getMessage()).build();
    }
}
