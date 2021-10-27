/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
