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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.MalformedRdfException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

/**
 * @author cabeer
 * @since 9/30/14
 */
@Provider
public class MalformedRdfExceptionMapper implements
        ExceptionMapper<MalformedRdfException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
            getLogger(MalformedRdfExceptionMapper.class);

    private static final int REASONABLE_LENGTH = 500;

    @Override
    public Response toResponse(final MalformedRdfException e) {
        debugException(this, e, LOGGER);
        final Link link = Link.fromUri(getConstraintUri(e)).rel(CONSTRAINED_BY.getURI()).build();
        final String msg = e.getMessage();
        if (msg.matches(".*org.*Exception: .*")) {
            return status(BAD_REQUEST).entity(msg.replaceAll("org.*Exception: ", "")).links(link).type(
                    TEXT_PLAIN_WITH_CHARSET).build();
        }
        return status(BAD_REQUEST).entity(msg).links(link).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

    private static String getConstraintUri(final MalformedRdfException e) {
        final int index = Math.min(e.getMessage().length(), REASONABLE_LENGTH);
        if (index > e.getMessage().length()) {
            LOGGER.debug("Truncating Link header to {} characters.", REASONABLE_LENGTH);
        }

        return "data:text/plain;base64," + Base64.encodeBase64String(e.getMessage().substring(0, index).getBytes());
    }
}
