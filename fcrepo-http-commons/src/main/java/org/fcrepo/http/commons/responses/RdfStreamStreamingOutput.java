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
package org.fcrepo.http.commons.responses;

import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static org.apache.jena.riot.Lang.JSONLD;
import static org.apache.jena.riot.Lang.RDFXML;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.RDFLanguages.getRegisteredLanguages;
import static org.apache.jena.riot.RDFFormat.RDFXML_PLAIN;
import static org.apache.jena.riot.RDFFormat.JSONLD_COMPACT_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_EXPAND_FLAT;
import static org.apache.jena.riot.RDFFormat.JSONLD_FLATTEN_FLAT;
import static org.apache.jena.riot.system.StreamRDFWriter.defaultSerialization;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.jena.riot.RiotException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.RdfStream;
import org.slf4j.Logger;

/**
 * Serializes an {@link RdfStream}.
 *
 * @author ajs6f
 * @since Oct 30, 2013
 */
public class RdfStreamStreamingOutput extends AbstractFuture<Void> implements
        StreamingOutput {

    private static final Logger LOGGER = getLogger(RdfStreamStreamingOutput.class);

    private static final String JSONLD_COMPACTED = "http://www.w3.org/ns/json-ld#compacted";

    private static final String JSONLD_FLATTENED = "http://www.w3.org/ns/json-ld#flattened";

    private final Lang format;

    private final MediaType mediaType;

    private final RdfStream rdfStream;

    private final Map<String, String> namespaces;

    /**
     * Normal constructor
     *
     * @param rdfStream the rdf stream
     * @param namespaces a namespace mapping
     * @param mediaType the media type
     */
    public RdfStreamStreamingOutput(final RdfStream rdfStream, final Map<String, String> namespaces,
            final MediaType mediaType) {
        super();

        if (LOGGER.isDebugEnabled()) {
            getRegisteredLanguages().forEach(format -> {
                LOGGER.debug("Discovered RDF writer writeableFormats: {} with mimeTypes: {}",
                        format.getName(), String.join(" ", format.getAltContentTypes()));
            });
        }
        final Lang format = contentTypeToLang(mediaType.getType() + "/" + mediaType.getSubtype());
        if (format != null) {
            this.format = format;
            this.mediaType = mediaType;
            LOGGER.debug("Setting up to serialize to: {}", format);
        } else {
            throw new WebApplicationException(NOT_ACCEPTABLE);
        }

        this.rdfStream = rdfStream;
        this.namespaces = namespaces;
    }

    @Override
    public void write(final OutputStream output) {
        try {
            LOGGER.debug("Serializing RDF stream in: {}", format);
            write(rdfStream, output, format, mediaType, namespaces);
        } catch (final IOException | RiotException e) {
            setException(e);
            LOGGER.debug("Error serializing RDF", e.getMessage());
            throw new WebApplicationException(e);
        }
    }

    private static void write(final RdfStream rdfStream,
                       final OutputStream output,
                       final Lang dataFormat,
                       final MediaType dataMediaType,
                       final Map<String, String> nsPrefixes) throws IOException {

        final RDFFormat format = defaultSerialization(dataFormat);

        // For formats that can be block-streamed (n-triples, turtle)
        if (format != null) {
            LOGGER.debug("Stream-based serialization of {}", dataFormat.toString());
            final StreamRDF stream = new SynchonizedStreamRDFWrapper(getWriterStream(output, format));
            stream.start();
            nsPrefixes.forEach(stream::prefix);
            rdfStream.forEach(stream::triple);
            stream.finish();

        // For formats that require analysis of the entire model and cannot be streamed directly (rdfxml, n3)
        } else {
            LOGGER.debug("Non-stream serialization of {}", dataFormat.toString());
            final Model model = rdfStream.collect(toModel());
            model.setNsPrefixes(nsPrefixes);
            // use block output streaming for RDFXML
            if (RDFXML.equals(dataFormat)) {
                RDFDataMgr.write(output, model.getGraph(), RDFXML_PLAIN);
            } else if (JSONLD.equals(dataFormat)) {
                final RDFFormat jsonldFormat = getFormatFromMediaType(dataMediaType);
                RDFDataMgr.write(output, model.getGraph(), jsonldFormat);
            } else {
                RDFDataMgr.write(output, model.getGraph(), dataFormat);
            }
        }
    }

    private static RDFFormat getFormatFromMediaType(final MediaType mediaType) {
        final String profile = mediaType.getParameters().getOrDefault("profile", "");
        if (profile.equals(JSONLD_COMPACTED)) {
            return JSONLD_COMPACT_FLAT;
        } else if (profile.equals(JSONLD_FLATTENED)) {
            return JSONLD_FLATTEN_FLAT;
        }
        return JSONLD_EXPAND_FLAT;
    }
}
