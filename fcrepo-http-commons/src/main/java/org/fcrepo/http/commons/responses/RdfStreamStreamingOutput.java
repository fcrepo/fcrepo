/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static com.apicatalog.jsonld.http.ProfileConstants.COMPACTED;
import static com.apicatalog.jsonld.http.ProfileConstants.EXPANDED;
import static com.apicatalog.jsonld.http.ProfileConstants.FLATTENED;
import static org.apache.jena.riot.Lang.JSONLD;
import static org.apache.jena.riot.Lang.RDFXML;
import static org.apache.jena.riot.RDFFormat.RDFXML_PLAIN;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.RDFLanguages.getRegisteredLanguages;
import static org.apache.jena.riot.system.StreamRDFWriter.defaultSerialization;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.serialization.QuadsToJsonld;
import com.apicatalog.rdf.api.RdfConsumerException;
import com.google.common.util.concurrent.AbstractFuture;
import jakarta.json.JsonValue;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NsIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.fcrepo.kernel.api.RdfStream;
import java.io.IOException;
import java.io.InputStream;
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
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * Serializes a {@link RdfStream}.
 *
 * @author ajs6f
 * @since Oct 30, 2013
 */
public class RdfStreamStreamingOutput extends AbstractFuture<Void> implements
        StreamingOutput {

    private static final Logger LOGGER = getLogger(RdfStreamStreamingOutput.class);

    private static final String JSONLD_EXPANDED = "http://www.w3.org/ns/json-ld#expanded";

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
                                             final Lang dataFormat, final MediaType dataMediaType,
                                             final Map<String, String> nsPrefixes) {
        final Model model = rdfStream.collect(toModel());
        final QuadsToJsonld q = new QuadsToJsonld().ordered(true).useNativeTypes(true).mode(JsonLdVersion.V1_1);

        model.setNsPrefixes(filterNamespacesToPresent(model, nsPrefixes));

        final String rdfFormat = getFormatFromMediaType(dataMediaType);
        LOGGER.debug("Stream-based serialization of {}", rdfFormat);

        // use block output streaming for RDFXML
        if (RDFXML.equals(dataFormat)) {
            RDFDataMgr.write(output, model.getGraph(), RDFXML_PLAIN);
        } else if (JSONLD.equals(dataFormat)) {
            final JsonWriterFactory writerFactory = getJsonWriter(model, q);
            final JsonArray expanded = getExpanded(q);
            writePayload(writerFactory, output, expanded, rdfFormat);
        } else {
            RDFDataMgr.write(output, model.getGraph(), dataFormat);
        }
    }

    private static JsonArray getExpanded(final QuadsToJsonld q) {
        final JsonArray expanded;

        try {
            expanded = q.toJsonLd();
        } catch (JsonLdError jsonLdError) {
            throw new WebApplicationException(jsonLdError);
        }
        return expanded;
    }

    private static JsonWriterFactory getJsonWriter(final Model model, final QuadsToJsonld q) {
        final DatasetGraph dsg = DatasetGraphFactory.wrap(model.getGraph());
        final Iterator<Quad> it = dsg.find();
        while (it.hasNext()) {
            final Quad qd = it.next();

            final String graphName = qd.isDefaultGraph() ? null : nodeAsTerm(qd.getGraph());
            final String s = nodeAsTerm(qd.getSubject());
            final String p = nodeAsTerm(qd.getPredicate());
            final Node o = qd.getObject();

            try {
                if (o.isLiteral()) {
                    final String lex = o.getLiteralLexicalForm();
                    final String lang = o.getLiteralLanguage();        // "" if none
                    String dt        = o.getLiteralDatatypeURI();      // may be null

                    // IMPORTANT: language-tagged literals must NOT carry a datatype
                    final String langOrNull = (lang != null && !lang.isEmpty()) ? lang : null;
                    if (langOrNull != null) {
                        dt = null;
                    } else if (dt == null) {
                        // ensure plain strings are treated as literals, not IRIs
                        dt = "http://www.w3.org/2001/XMLSchema#string";
                    }

                    // LOG THE VALUES YOU ACTUALLY PASS
                    LOGGER.info("TITLE quad -> pass lex='{}', lang={}, dt={}", lex, langOrNull, dt);

                    // LITERAL OVERLOAD (7 args): value, datatype, language, direction, graph
                    q.quad(s, p, lex, dt, langOrNull, null, graphName);

                } else {
                    // NODE OVERLOAD – only for IRIs/blank nodes
                    q.quad(s, p, nodeAsTerm(o), null, null, null, graphName);
                }
            } catch (RdfConsumerException e) {
                throw new WebApplicationException(e);
            }
        }

        // compact output; omit PRETTY_PRINTING key entirely to ensure no pretty-print
        return Json.createWriterFactory(java.util.Collections.emptyMap());
    }

    /**
     * Adds the context to a JSON Array which is a requirement of the compact JSON-LD profile
     * @param expanded JSON Array expanded profile
     * @return compacted JsonStructure with context applied
     * @throws IOException
     * @throws JsonLdError
     */
    private static JsonStructure compactWithContext(final JsonArray expanded)
            throws IOException, JsonLdError {

        try (InputStream ctx = Objects.requireNonNull(
                RdfStreamStreamingOutput.class.getResourceAsStream("/context.jsonld"),
                "context.jsonld not on classpath")) {

            return JsonLd.compact(
                    JsonDocument.of(expanded),
                    JsonDocument.of(ctx)
            ).get();
        }
    }

    /**
     * Writes JSON-LD output and supports flattened, expanded and compacted (default) profiles
     *
     * @param writerFactory JSON Writer to eventually write to
     * @param output stream for the JSON Writer
     * @param expanded JSON Array representation of rhe JSON to be written in expanded format
     * @param rdfFormat profile requested for the JSON-LD (expanded, flattened, compacted)
     */
    private static void writePayload(
            final JsonWriterFactory writerFactory,
            final OutputStream output,
            final JsonArray expanded,
            final String rdfFormat) {

        final JsonValue payload;

        try {
            switch (rdfFormat) {
                case EXPANDED -> payload = expanded;

                case FLATTENED -> payload =
                        JsonLd.flatten(JsonDocument.of(expanded)).get();

                case COMPACTED -> payload = compactWithContext(expanded);

                default -> payload = compactWithContext(expanded);
            }
        } catch (JsonLdError | IOException e) {
            throw new WebApplicationException(e);
        }

        try (JsonWriter writer = writerFactory.createWriter(output)) {
            writer.write(payload);
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
        if (profile.equals(JSONLD_EXPANDED)) {
            return EXPANDED;
        } else if (profile.equals(JSONLD_FLATTENED)) {
            return FLATTENED;
        }
        return COMPACTED;
    }
}
