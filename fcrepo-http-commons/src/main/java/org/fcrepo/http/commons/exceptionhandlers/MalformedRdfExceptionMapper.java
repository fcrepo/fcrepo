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

import org.apache.commons.codec.binary.Base64;
import org.fcrepo.kernel.exception.MalformedRdfException;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.fcrepo.kernel.RdfLexicon.CONSTRAINED_BY;

/**
 * @author cabeer
 * @since 9/30/14
 */
@Provider
public class MalformedRdfExceptionMapper implements ExceptionMapper<MalformedRdfException> {

    @Override
    public Response toResponse(final MalformedRdfException e) {
        final Link link = Link.fromUri(getConstraintUri(e)).rel(CONSTRAINED_BY.getURI()).build();
        return status(CONFLICT).entity(e.getMessage()).links(link).build();
    }

    private static String getConstraintUri(final MalformedRdfException e) {
        return "data:text/plain;base64," + Base64.encodeBase64String(e.getMessage().getBytes());
    }
}
