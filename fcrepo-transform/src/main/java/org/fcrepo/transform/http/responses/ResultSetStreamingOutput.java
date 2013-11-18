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
import static com.hp.hpl.jena.query.ResultSetFormatter.toModel;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_UNKNOWN;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.jena.riot.WebContent;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
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
    public void write(final OutputStream entityStream) throws IOException {
        final ResultsFormat resultsFormat = getResultsFormat(mediaType);

        try {
            if (resultsFormat == FMT_UNKNOWN) {
                new GraphStoreStreamingOutput(create(toModel(results)), mediaType)
                        .write(entityStream);
            } else {
                ResultSetFormatter.output(entityStream, results, resultsFormat);
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
            case WebContent.contentTypeTextTSV:
                return ResultsFormat.FMT_RS_TSV;

            case WebContent.contentTypeTextCSV:
                return ResultsFormat.FMT_RS_CSV;

            case WebContent.contentTypeSSE:
                return ResultsFormat.FMT_RS_SSE;

            case WebContent.contentTypeTextPlain:
                return ResultsFormat.FMT_TEXT;

            case WebContent.contentTypeResultsJSON:
                return ResultsFormat.FMT_RS_JSON;

            case WebContent.contentTypeResultsXML:
                return ResultsFormat.FMT_RS_XML;

            case WebContent.contentTypeResultsBIO:
                return ResultsFormat.FMT_RS_BIO;

            case WebContent.contentTypeTurtle:
            case WebContent.contentTypeTurtleAlt1:
            case WebContent.contentTypeTurtleAlt2:
                return ResultsFormat.FMT_RDF_TTL;

            case WebContent.contentTypeN3:
            case WebContent.contentTypeN3Alt1:
            case WebContent.contentTypeN3Alt2:
                return ResultsFormat.FMT_RDF_N3;

            case WebContent.contentTypeNTriples:
                return ResultsFormat.FMT_RDF_NT;

            case WebContent.contentTypeRDFXML:
                return ResultsFormat.FMT_RDF_XML;

        }

        return ResultsFormat.FMT_UNKNOWN;
    }
}
