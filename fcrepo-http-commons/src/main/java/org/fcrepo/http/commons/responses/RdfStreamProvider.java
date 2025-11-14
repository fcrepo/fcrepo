/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.responses;

import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import org.slf4j.Logger;
import org.fcrepo.kernel.api.rdf.RdfNamespaceRegistry;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

/**
 * Provides serialization for streaming RDF results.
 *
 * @author ajs6f
 * @since Nov 19, 2013
 */
@Provider
@Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, TEXT_PLAIN, JSON_LD})
public class RdfStreamProvider implements MessageBodyWriter<RdfNamespacedStream> {

    private static final Logger LOGGER = getLogger(RdfStreamProvider.class);

    @Inject
    private RdfNamespaceRegistry registry;

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        LOGGER.debug(
                "Checking to see if we can serialize type: {} to mimeType: {}",
                type.getName(), mediaType.toString());
        if (!RdfNamespacedStream.class.isAssignableFrom(type)) {
            return false;
        }
        if (mediaType.equals(TEXT_HTML_TYPE)
                || (mediaType.getType().equals("application") && mediaType
                        .getSubtype().equals("html"))) {
            LOGGER.debug("Was asked for an HTML mimeType, returning false.");
            return false;
        }
        LOGGER.debug("Assuming that this is an attempt to retrieve RDF, returning true.");
        return true;
    }

    @Override
    public long getSize(final RdfNamespacedStream t, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        // We do not know how long the stream is
        return -1;
    }

    @Override
    public void writeTo(final RdfNamespacedStream nsStream, final Class<?> type,
        final Type genericType, final Annotation[] annotations,
        final MediaType mediaType,
        final MultivaluedMap<String, Object> httpHeaders,
        final OutputStream entityStream) {
        LOGGER.debug("Serializing an RdfStream to mimeType: {}", mediaType);
        final var namespaces = registry.getNamespaces();
        nsStream.namespaces.entrySet().stream().filter(entry -> !namespaces.containsValue(entry.getValue()))
                .forEach(entry -> namespaces.put(entry.getKey(), entry.getValue()));
        final RdfStreamStreamingOutput streamOutput = new RdfStreamStreamingOutput(nsStream.stream,
                namespaces, mediaType);
        streamOutput.write(entityStream);
    }
}
