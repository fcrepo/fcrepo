/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.http.commons.domain;

import static javax.ws.rs.core.Variant.mediaTypes;
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
import static org.apache.jena.riot.WebContent.contentTypeTurtleAlt2;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

/**
 * This is a convenience class carrying the various RDF content-type values as
 * Strings and MediaTypes, after the fashion of the constants available on
 * javax.ws.rs.core.MediaType
 *
 * @author ba2213
 */
public abstract class RDFMediaType extends MediaType {

    public static final String N3 = contentTypeN3;

    public static final MediaType N3_TYPE = typeFromString(N3);

    public static final String N3_ALT1 = contentTypeN3Alt1;

    public static final MediaType N3_ALT1_TYPE = typeFromString(N3_ALT1);

    public static final String N3_ALT2 = contentTypeN3Alt2;

    public static final MediaType N3_ALT2_TYPE = typeFromString(N3_ALT2);

    public static final String TURTLE = contentTypeTurtle;

    public static final MediaType TURTLE_TYPE = typeFromString(TURTLE);

    public static final String TURTLE_X = contentTypeTurtleAlt2;

    public static final MediaType TURTLE_X_TYPE = typeFromString(TURTLE_X);

    public static final String RDF_XML = contentTypeRDFXML;

    public static final MediaType RDF_XML_TYPE = typeFromString(RDF_XML);

    public static final String NTRIPLES = contentTypeNTriples;

    public static final MediaType NTRIPLES_TYPE = typeFromString(NTRIPLES);

    public static final List<Variant> POSSIBLE_RDF_VARIANTS = mediaTypes(
            TURTLE_TYPE, N3_TYPE, N3_ALT2_TYPE, RDF_XML_TYPE, NTRIPLES_TYPE, APPLICATION_XML_TYPE, TEXT_PLAIN_TYPE,
            TURTLE_X_TYPE).add().build();

    public static final String POSSIBLE_RDF_RESPONSE_VARIANTS_STRING[] = {
        TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, TEXT_PLAIN, APPLICATION_XML, TURTLE_X };

    public static final String TSV = contentTypeTextTSV;

    public static final MediaType TSV_TYPE = typeFromString(TSV);

    public static final String CSV = contentTypeTextCSV;

    public static final MediaType CSV_TYPE = typeFromString(CSV);

    public static final String SSE = contentTypeSSE;

    public static final MediaType SSE_TYPE = typeFromString(SSE);

    public static final String PLAIN = contentTypeTextPlain;

    public static final MediaType PLAIN_TYPE = typeFromString(PLAIN);

    public static final String RESULTS_JSON = contentTypeResultsJSON;

    public static final MediaType RESULTS_JSON_TYPE = typeFromString(RESULTS_JSON);

    public static final String RESULTS_XML = contentTypeResultsXML;

    public static final MediaType RESULTS_XML_TYPE = typeFromString(RESULTS_XML);

    public static final String RESULTS_BIO = contentTypeResultsBIO;

    public static final MediaType RESULTS_BIO_TYPE = typeFromString(RESULTS_BIO);

    public static final List<Variant> POSSIBLE_SPARQL_RDF_VARIANTS = mediaTypes(
            TSV_TYPE, CSV_TYPE, SSE_TYPE, PLAIN_TYPE, RESULTS_JSON_TYPE,
            RESULTS_XML_TYPE, RESULTS_BIO_TYPE, RDF_XML_TYPE, NTRIPLES_TYPE,
            N3_TYPE, N3_ALT1_TYPE, N3_ALT2_TYPE, TURTLE_TYPE).add().build();

    private static MediaType typeFromString(final String type) {
        return new MediaType(type.split("/")[0], type.split("/")[1]);
    }

}
