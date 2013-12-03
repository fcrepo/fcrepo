/**
 * Copyright 2013 DuraSpace, Inc.
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

import static com.hp.hpl.jena.query.DatasetFactory.create;
import static com.hp.hpl.jena.query.ResultSetFormatter.output;
import static com.hp.hpl.jena.query.ResultSetFormatter.toModel;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_N3;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_NT;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_TTL;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_XML;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_BIO;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_CSV;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_JSON;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_SSE;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_TSV;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_XML;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_TEXT;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_UNKNOWN;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt1;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt2;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeResultsBIO;
import static org.apache.jena.riot.WebContent.contentTypeResultsJSON;
import static org.apache.jena.riot.WebContent.contentTypeResultsXML;
import static org.apache.jena.riot.WebContent.contentTypeSSE;
import static org.apache.jena.riot.WebContent.contentTypeTextCSV;
import static org.apache.jena.riot.WebContent.contentTypeTextPlain;
import static org.apache.jena.riot.WebContent.contentTypeTextTSV;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.apache.jena.riot.WebContent.contentTypeTurtleAlt1;
import static org.apache.jena.riot.WebContent.contentTypeTurtleAlt2;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.util.concurrent.AbstractFuture;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import org.fcrepo.http.commons.responses.GraphStoreStreamingOutput;

/**
 * Stream the results of a SPARQL Query
 */
public class ResultSetStreamingOutput extends AbstractFuture<Void> implements StreamingOutput {
    private final ResultSet results;
    private final MediaType mediaType;

    private static final Void finishedMarker = null;

    /**
     * Stream the results of a SPARQL Query with the given MediaType
     * @param results
     * @param mediaType
     */
    public ResultSetStreamingOutput(final ResultSet results, final MediaType mediaType) {
        this.mediaType = mediaType;
        this.results = results;
    }

    /**
     *
     * @param entityStream
     * @throws IOException
     */
    @Override
    public void write(final OutputStream entityStream) throws IOException {
        final ResultsFormat resultsFormat = getResultsFormat(mediaType);

        try {
            if (resultsFormat == FMT_UNKNOWN) {
                new GraphStoreStreamingOutput(create(toModel(results)),
                        mediaType).write(entityStream);
            } else {
                output(entityStream, results, resultsFormat);
            }
        } finally {
            set(finishedMarker);
        }
    }

    /**
     * Map the HTTP MediaType to a SPARQL ResultsFormat
     * @param mediaType
     * @return
     */
    public static ResultsFormat getResultsFormat(final MediaType mediaType) {
        switch (mediaType.toString()) {
            case contentTypeTextTSV:
                return FMT_RS_TSV;

            case contentTypeTextCSV:
                return FMT_RS_CSV;

            case contentTypeSSE:
                return FMT_RS_SSE;

            case contentTypeTextPlain:
                return FMT_TEXT;

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

            case contentTypeN3:
            case contentTypeN3Alt1:
            case contentTypeN3Alt2:
                return FMT_RDF_N3;

            case contentTypeNTriples:
                return FMT_RDF_NT;

            case contentTypeRDFXML:
                return FMT_RDF_XML;
        }
        return FMT_UNKNOWN;
    }
}
