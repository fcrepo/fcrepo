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
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.jena.riot.RiotException;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NsIterator;
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

    private static final String RDF_TYPE = RDF_NAMESPACE + "type";

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
        } catch (final RiotException e) {
            setException(e);
            LOGGER.debug("Error serializing RDF", e.getMessage());
            throw new WebApplicationException(e);
        }
    }

    private static void write(final RdfStream rdfStream,
                       final OutputStream output,
                       final Lang dataFormat,
                       final MediaType dataMediaType,
                       final Map<String, String> nsPrefixes) {

        final RDFFormat format = defaultSerialization(dataFormat);

        // For formats that can be block-streamed (n-triples, turtle)
        if (format != null) {
            LOGGER.debug("Stream-based serialization of {}", dataFormat.toString());
            if (RDFFormat.NTRIPLES.equals(format)) {
                serializeNTriples(rdfStream, format, output);
            } else {
                serializeBlockStreamed(rdfStream, output, format, nsPrefixes);
            }
        // For formats that require analysis of the entire model and cannot be streamed directly (rdfxml, n3)
        } else {
            LOGGER.debug("Non-stream serialization of {}", dataFormat.toString());
            serializeNonStreamed(rdfStream, output, dataFormat, dataMediaType, nsPrefixes);
        }
    }

    private static void serializeNTriples(final RdfStream rdfStream, final RDFFormat format,
            final OutputStream output) {
        final StreamRDF stream = new SynchonizedStreamRDFWrapper(getWriterStream(output, format.getLang()));
        stream.start();
        rdfStream.forEach(stream::triple);
        stream.finish();
    }

    private static void serializeBlockStreamed(final RdfStream rdfStream, final OutputStream output,
            final RDFFormat format, final Map<String, String> nsPrefixes) {

        final Set<String> namespacesPresent = new HashSet<>();

        final StreamRDF stream = new SynchonizedStreamRDFWrapper(getWriterStream(output, format.getLang()));
        stream.start();
        // Must read the rdf stream before writing out ns prefixes, otherwise the prefixes come after the triples
        final List<Triple> tripleList = rdfStream.peek(t -> {
            // Collect the namespaces present in the RDF stream, using the same
            // criteria for where to look that jena's model.listNameSpaces() does
            namespacesPresent.add(t.getPredicate().getNameSpace());
            if (RDF_TYPE.equals(t.getPredicate().getURI()) && t.getObject().isURI()) {
                namespacesPresent.add(t.getObject().getNameSpace());
            }
        }).collect(Collectors.toList());

        nsPrefixes.forEach((prefix, uri) -> {
            // Only add namespace prefixes if the namespace is present in the rdf stream
            if (namespacesPresent.contains(uri)) {
                stream.prefix(prefix, uri);
            }
        });
        tripleList.forEach(stream::triple);
        stream.finish();
    }

    private static void serializeNonStreamed(final RdfStream rdfStream, final OutputStream output,
            final Lang dataFormat, final MediaType dataMediaType, final Map<String, String> nsPrefixes) {
        final Model model = rdfStream.collect(toModel());

        model.setNsPrefixes(filterNamespacesToPresent(model, nsPrefixes));
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

    /**
     * Filters the map of namespace prefix mappings to just those containing namespace URIs present in the model
     *
     * @param model model
     * @param nsPrefixes map of namespace to uris
     * @return nsPrefixes filtered to namespaces found in the model
     */
    private static Map<String, String> filterNamespacesToPresent(final Model model,
            final Map<String, String> nsPrefixes) {
        final Map<String, String> resultNses = new HashMap<>();
        final Set<Entry<String, String>> nsSet = nsPrefixes.entrySet();
        final NsIterator nsIt = model.listNameSpaces();
        while (nsIt.hasNext()) {
            final String ns = nsIt.next();

            final Optional<Entry<String, String>> nsOpt = nsSet.stream()
                    .filter(nsEntry -> nsEntry.getValue().equals(ns))
                    .findFirst();
            if (nsOpt.isPresent()) {
                final Entry<String, String> nsMatch = nsOpt.get();
                resultNses.put(nsMatch.getKey(), nsMatch.getValue());
            }
        }

        return resultNses;
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
