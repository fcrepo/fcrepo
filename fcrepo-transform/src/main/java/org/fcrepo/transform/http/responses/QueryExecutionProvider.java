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
package org.fcrepo.transform.http.responses;

import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_UNKNOWN;
import static java.util.Collections.singletonList;
import static org.fcrepo.transform.http.responses.ResultSetStreamingOutput.getResultsFormat;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Helper for writing QueryExecutions results out in a variety
 * of serialization formats.
 *
 * @author cbeer
 */
@Provider
@Component
public class QueryExecutionProvider implements MessageBodyWriter<QueryExecution> {

    private static final Logger LOGGER = getLogger(QueryExecutionProvider.class);

    private static final ResultSetStreamingOutput resultSetStreamingOutput = new ResultSetStreamingOutput();

    @Override
    public void writeTo(final QueryExecution qexec, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) {

        LOGGER.debug("Writing a response for: {} with MIMEtype: {}", qexec,
                        mediaType);

        // add standard headers
        httpHeaders.put("Content-type", singletonList(mediaType.toString()));

        try {
            final ResultSet resultSet = qexec.execSelect();

            resultSetStreamingOutput.writeTo(resultSet, type, genericType,
                    annotations, mediaType, httpHeaders, entityStream);
        } finally {
            qexec.close();
        }
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {

        // we can return a result for any MIME type that Jena can serialize
        final Boolean appropriateResultType =
            getResultsFormat(mediaType) != FMT_UNKNOWN;
        return appropriateResultType
                && (QueryExecution.class.isAssignableFrom(type) || QueryExecution.class
                        .isAssignableFrom(genericType.getClass()));
    }

    @Override
    public long getSize(final QueryExecution rdf, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        // we don't know in advance how large the result might be
        return -1;
    }

}
