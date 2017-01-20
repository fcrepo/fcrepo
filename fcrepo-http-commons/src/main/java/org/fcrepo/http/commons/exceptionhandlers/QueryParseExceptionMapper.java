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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

import org.apache.jena.query.QueryParseException;


/**
 * Handles Sparql query parsing exceptions thrown when querying or updating.
 *
 * @author whikloj
 * @since September 9, 2014
 */
@Provider
public class QueryParseExceptionMapper implements
        ExceptionMapper<QueryParseException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(QueryParseExceptionMapper.class);

    @Override
    public Response toResponse(final QueryParseException e) {

        LOGGER.error("Captured a query parse exception {}", e.getMessage());
        debugException(this, e, LOGGER);
        if (e.getMessage().matches(".* Unresolved prefixed name: .*")) {
            final Pattern namespacePattern =
                Pattern.compile("Unresolved prefixed name: (\\w+:\\w+)");
            final Matcher namespaceMatch =
                namespacePattern.matcher(e.getMessage());
            if (namespaceMatch.find()) {
                final String msg =
                    String.format(
                        "There are one or more undefined namespace(s) in your request [ %s ], " +
                        "please define them before retrying",
                        namespaceMatch.group(1));
                return status(BAD_REQUEST).entity(msg).type(TEXT_PLAIN_WITH_CHARSET).build();
            }
        }

        return status(BAD_REQUEST).entity(e.getMessage()).type(TEXT_PLAIN_WITH_CHARSET).build();
    }

}
