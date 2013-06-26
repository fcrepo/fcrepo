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

package org.fcrepo.http;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import org.apache.jena.riot.WebContent;

/**
 * This is a convenience class carrying the various RDF content-type values as
 * Strings and MediaTypes, after the fashion of the constants available on
 * javax.ws.rs.core.MediaType
 * 
 * @author ba2213
 */
public abstract class RDFMediaType extends MediaType {

    public static final String N3 = WebContent.contentTypeN3;

    public static final MediaType N3_TYPE = new MediaType(N3.split("/")[0], N3
            .split("/")[1]);

    public static final String N3_ALT1 = WebContent.contentTypeN3Alt1;

    public static final MediaType N3_ALT1_TYPE = new MediaType(N3_ALT1
            .split("/")[0], N3_ALT1.split("/")[1]);

    public static final String N3_ALT2 = WebContent.contentTypeN3Alt2;

    public static final MediaType N3_ALT2_TYPE = new MediaType(N3_ALT2
            .split("/")[0], N3_ALT2.split("/")[1]);

    public static final String TURTLE = WebContent.contentTypeTurtle;

    public static final MediaType TURTLE_TYPE = new MediaType(
            TURTLE.split("/")[0], TURTLE.split("/")[1]);

    public static final String RDF_XML = WebContent.contentTypeRDFXML;

    public static final MediaType RDF_XML_TYPE = new MediaType(RDF_XML
            .split("/")[0], RDF_XML.split("/")[1]);

    public static final String RDF_JSON = WebContent.contentTypeRDFJSON;

    public static final MediaType RDF_JSON_TYPE = new MediaType(RDF_JSON
            .split("/")[0], RDF_JSON.split("/")[1]);

    public static final String NTRIPLES = WebContent.contentTypeNTriples;

    public static final MediaType NTRIPLES_TYPE = new MediaType(NTRIPLES
            .split("/")[0], NTRIPLES.split("/")[1]);

    public static final String TRI_G = WebContent.contentTypeTriG;

    public static final MediaType TRI_G_TYPE = new MediaType(
            TRI_G.split("/")[0], TRI_G.split("/")[1]);

    public static final String NQUADS = WebContent.contentTypeNQuads;

    public static final MediaType NQUADS_TYPE = new MediaType(
            NQUADS.split("/")[0], NQUADS.split("/")[1]);

    public static final List<Variant> POSSIBLE_RDF_VARIANTS = Variant
            .mediaTypes(N3_TYPE, N3_ALT1_TYPE, N3_ALT2_TYPE, TURTLE_TYPE,
                    RDF_XML_TYPE, RDF_JSON_TYPE, NTRIPLES_TYPE, TRI_G_TYPE,
                    NQUADS_TYPE).add().build();
}
