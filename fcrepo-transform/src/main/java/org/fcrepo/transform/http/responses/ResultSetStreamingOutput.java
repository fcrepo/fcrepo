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

import static com.hp.hpl.jena.query.ResultSetFormatter.output;
import static com.hp.hpl.jena.query.ResultSetFormatter.toModel;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_NT;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_TTL;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_XML;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_BIO;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_CSV;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_JSON;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_TSV;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_XML;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_UNKNOWN;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeResultsBIO;
import static org.apache.jena.riot.WebContent.contentTypeResultsJSON;
import static org.apache.jena.riot.WebContent.contentTypeResultsXML;
import static org.apache.jena.riot.WebContent.contentTypeTextCSV;
import static org.apache.jena.riot.WebContent.contentTypeTextTSV;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.apache.jena.riot.WebContent.contentTypeTurtleAlt1;
import static org.apache.jena.riot.WebContent.contentTypeTurtleAlt2;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.jena.riot.Lang;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;

/**
 * Stream the results of a SPARQL Query
 *
 * @author cbeer
 */
@Provider
@Produces({contentTypeTextTSV, contentTypeTextCSV, contentTypeResultsJSON,
        contentTypeResultsXML, contentTypeResultsBIO, contentTypeTurtle,
        contentTypeNTriples, contentTypeRDFXML})
public class ResultSetStreamingOutput implements MessageBodyWriter<ResultSet> {



    @Override
    public boolean isWriteable(final Class<?> type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType) {
        final ResultsFormat resultsFormat = getResultsFormat(mediaType);

        if (resultsFormat == FMT_UNKNOWN) {
            final Lang format = contentTypeToLang(mediaType.toString());

            return format != null;
        }
        return true;
    }

    @Override
    public long getSize(final ResultSet resultSet,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final ResultSet resultSet,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws WebApplicationException {
        final ResultsFormat resultsFormat = getResultsFormat(mediaType);

        if (resultsFormat == FMT_UNKNOWN) {
            final String format = contentTypeToLang(mediaType.toString()).getName().toUpperCase();
            final Model model = toModel(resultSet);
            model.write(entityStream, format);
        } else {
            output(entityStream, resultSet, resultsFormat);
        }
    }

    /**
     * Map the HTTP MediaType to a SPARQL ResultsFormat
     * @param mediaType
     * @return SPARQL {@link ResultsFormat} for the given {@link MediaType}
     */
    public static ResultsFormat getResultsFormat(final MediaType mediaType) {
        switch (mediaType.toString()) {
            case contentTypeTextTSV:
                return FMT_RS_TSV;

            case contentTypeTextCSV:
                return FMT_RS_CSV;

            case contentTypeResultsJSON:
                return FMT_RS_JSON;

            case contentTypeResultsXML:
                return FMT_RS_XML;

            case contentTypeResultsBIO:
                return FMT_RS_BIO;

            case contentTypeTurtle:
            case contentTypeTurtleAlt1:
            case contentTypeTurtleAlt2:
                return FMT_RDF_TTL;

            case contentTypeNTriples:
                return FMT_RDF_NT;

            case contentTypeRDFXML:
                return FMT_RDF_XML;
            default:
                return FMT_UNKNOWN;
        }
    }
}
