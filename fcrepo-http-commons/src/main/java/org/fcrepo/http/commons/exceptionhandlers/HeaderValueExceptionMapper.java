/**
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

import org.glassfish.jersey.message.internal.HeaderValueException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.status;

/**
 * If a client-provided header value fails to parse, return an HTTP 400 Bad Request.
 *
 * @author awoods
 * @since 2015-08-06
 */
@Provider
public class HeaderValueExceptionMapper implements ExceptionMapper<HeaderValueException> {

    @Override
    public Response toResponse(final HeaderValueException ex) {
        return status(BAD_REQUEST).entity(ex.getMessage() + " ...should value be quoted?").build();
    }
}
