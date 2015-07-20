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

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import org.fcrepo.kernel.api.exception.IncorrectTripleSubjectException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

/**
 * @author ajs6f
 * @since 2015-07-15
 */
@Provider
public class IncorrectTripleSubjectExceptionMapper extends ConstraintExceptionMapper<IncorrectTripleSubjectException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(final IncorrectTripleSubjectException e) {

        final Link link = buildConstraintLink(e, uriInfo);
        final String msg = e.getMessage();
        return status(FORBIDDEN).entity(msg).links(link).build();
    }
}
