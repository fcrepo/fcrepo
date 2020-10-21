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

import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.MultipleConstraintViolationException;
import org.slf4j.Logger;

/**
 * Mapper to display all the various constrainedby links and messages.
 * @author whikloj
 */
@Provider
public class MultipleConstraintViolationExceptionMapper extends
        ConstraintExceptionMapper<MultipleConstraintViolationException> implements ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(MultipleConstraintViolationExceptionMapper.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private ServletContext context;

    @Override
    public Response toResponse(final MultipleConstraintViolationException e) {
        debugException(this, e, LOGGER);

        final String msg = e.getMessage();
        final Response.ResponseBuilder response = status(CONFLICT).entity(msg).type(TEXT_PLAIN_WITH_CHARSET);
        final Link[] constraintLinks = e.getExceptionTypes().stream().map(ConstraintViolationException::getClass)
                .distinct().map(c -> buildConstraintLink(c, context, uriInfo))
                .toArray(Link[]::new);
        return response.links(constraintLinks).build();
    }

}
