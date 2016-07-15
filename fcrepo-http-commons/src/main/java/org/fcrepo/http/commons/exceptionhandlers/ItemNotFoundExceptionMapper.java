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

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.ItemNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * Translate jcr ItemNotFoundException to HTTP 404 Not Found
 *
 * @author lsitu
 */
@Provider
public class ItemNotFoundExceptionMapper implements
        ExceptionMapper<ItemNotFoundException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(ItemNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(final ItemNotFoundException e) {
        debugException(this, e, LOGGER);
        return status(NOT_FOUND).build();
    }
}
