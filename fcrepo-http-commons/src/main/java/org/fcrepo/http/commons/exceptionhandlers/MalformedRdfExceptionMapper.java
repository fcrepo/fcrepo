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

import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.MalformedRdfException;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

/**
 * @author cabeer
 * @since 9/30/14
 */
@Provider
public class MalformedRdfExceptionMapper extends FedoraExceptionMapper<MalformedRdfException> {

    private static final Logger LOGGER =
            getLogger(MalformedRdfExceptionMapper.class);

    private static final int REASONABLE_LENGTH = 500;

    @Override
    protected ResponseBuilder links(final ResponseBuilder builder, final MalformedRdfException e) {
        final Link link = Link.fromUri(getConstraintUri(e)).rel(CONSTRAINED_BY.getURI()).build();
        return builder.links(link);
    }

    @Override
    protected ResponseBuilder entity(final ResponseBuilder builder, final MalformedRdfException e) {
        final String msg = e.getMessage();
        return builder.entity(msg.matches(".*org.*Exception: .*") ? msg.replaceAll("org.*Exception: ", "") : msg);
    }

    private static String getConstraintUri(final MalformedRdfException e) {
        final int index = Math.min(e.getMessage().length(), REASONABLE_LENGTH);
        if (index > e.getMessage().length()) {
            LOGGER.debug("Truncating Link header to {} characters.", REASONABLE_LENGTH);
        }

        return "data:text/plain;base64," + Base64.encodeBase64String(e.getMessage().substring(0, index).getBytes());
    }
}
