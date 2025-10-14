/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static org.apache.jena.riot.Lang.JSONLD;
import static org.apache.jena.riot.Lang.RDFXML;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.RDFLanguages.getRegisteredLanguages;
import static org.apache.jena.riot.RDFFormat.RDFXML_PLAIN;
import static org.apache.jena.riot.system.StreamRDFWriter.defaultSerialization;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static com.apicatalog.jsonld.http.ProfileConstants.COMPACTED;
import static com.apicatalog.jsonld.http.ProfileConstants.EXPANDED;
import static com.apicatalog.jsonld.http.ProfileConstants.FLATTENED;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.serialization.QuadsToJsonld;
import com.apicatalog.rdf.api.RdfConsumerException;
import jakarta.json.Json;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotException;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NsIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
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
            LOGGER.debug("Error serializing RDF: {}", e.getMessage());
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
        }).toList();

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

        final String rdfFormat = getFormatFromMediaType(dataMediaType);
        LOGGER.debug("Stream-based serialization of {}", rdfFormat);

        final DatasetGraph dsg = DatasetGraphFactory.wrap(model.getGraph());
        final QuadsToJsonld q = new QuadsToJsonld().ordered(true).useNativeTypes(true).mode(JsonLdVersion.V1_1);

        // use block output streaming for RDFXML
        if (RDFXML.equals(dataFormat)) {
            RDFDataMgr.write(output, model.getGraph(), RDFXML_PLAIN);
        } else if (JSONLD.equals(dataFormat)) {
            final Iterator<Quad> it = dsg.find();

            while (it.hasNext()) {
                final Quad quad = it.next();

                final String graphName = quad.isDefaultGraph() ? null : nodeAsTerm(quad.getGraph());
                final String s = nodeAsTerm(quad.getSubject());
                final String p = nodeAsTerm(quad.getPredicate());
                final Node o = quad.getObject();
                try {
                    if (o.isLiteral()) {
                    final var lit = o.getLiteral();
                        q.quad(
                                s, p,
                                lit.getLexicalForm(),
                                lit.getDatatypeURI(),
                                lit.language(),
                                null,
                                graphName
                        );
                    } else {
                        q.quad(s, p, nodeAsTerm(o), null, null, null, graphName);
                    }
                } catch (RdfConsumerException e) {
                    LOGGER.debug("Error processing RDF: {}", e.getMessage());
                    throw new WebApplicationException(e);
                }
            }

            try {
                final JsonWriterFactory writerFactory =
                        Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, false));
                JsonStructure jsonResponse = q.toJsonLd();  // default to expanded
                if (rdfFormat.equals(FLATTENED)) {
                    jsonResponse = JsonLd.flatten(JsonDocument.of(jsonResponse)).get();
                } else if (rdfFormat.equals(COMPACTED)) {
                    jsonResponse = JsonLd.compact(
                            JsonDocument.of(jsonResponse),
                            JsonDocument.of(Objects.requireNonNull(
                                    RdfStreamStreamingOutput.class.getResourceAsStream("/context.jsonld")))
                    ).get();
                }

                try (JsonWriter writer = writerFactory.createWriter(output)) {
                    writer.write(jsonResponse);
                }
            } catch (JsonLdError e) {
                LOGGER.debug("Error processing JSON: {}", e.getMessage());
                throw new WebApplicationException(e);
            }

        } else {
            RDFDataMgr.write(output, model.getGraph(), dataFormat);
        }
    }

    /**
     * Utility method to convert Nodes to String representations to support Titanium’s QuadsToJsonld.quad(...)
     * method which wants plain string values
     *
     *  @param node Jena Node
     * @return String representation of Node
     */
    private
    static String nodeAsTerm(final Node node) {
        if (node.isURI()) {
            return node.getURI();
        }
        if (node.isBlank()) {
            return "_:" + node.getBlankNodeLabel();
        }
        if (node.isLiteral()) {
            return node.getLiteralLexicalForm();
        }
        if (node.isVariable()) {
            return "?" + node.getName();
        }
        throw new IllegalArgumentException("Unsupported node type: " + node);
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
            nsOpt.ifPresent(nsMatch -> resultNses.put(nsMatch.getKey(), nsMatch.getValue()));
        }

        return resultNses;
    }

    private static String getFormatFromMediaType(final MediaType mediaType) {
        final String profile = mediaType.getParameters().getOrDefault("profile", "");
        if (profile.equals(JSONLD_COMPACTED)) {
            return COMPACTED;
        } else if (profile.equals(JSONLD_FLATTENED)) {
            return FLATTENED;
        }
        return EXPANDED;
    }
}
