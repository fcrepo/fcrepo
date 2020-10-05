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

import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.slf4j.Logger;


/**
 * Catch PathNotFoundException
 *
 * @author robyj
 */
@Provider
public class PathNotFoundExceptionMapper implements
        ExceptionMapper<PathNotFoundException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(PathNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(final PathNotFoundException e) {
        debugException(this, e, LOGGER);
        return Response.status(Response.Status.NOT_FOUND).
                entity("Error: " + e.getMessage()).build();
    }
}

