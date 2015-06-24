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

import org.fcrepo.kernel.exception.ServerManagedPropertyException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;

/**
 * @author cabeer
 * @author whikloj
 * @since 10/1/14
 */
@Provider
public class ServerManagedPropertyExceptionMapper extends ConstraintExceptionMapper<ServerManagedPropertyException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(final ServerManagedPropertyException e) {
        final Link link = buildConstraintLink(e, uriInfo);
        final String msg = e.getMessage();
        return status(CONFLICT).entity(msg).links(link).build();
    }
}
