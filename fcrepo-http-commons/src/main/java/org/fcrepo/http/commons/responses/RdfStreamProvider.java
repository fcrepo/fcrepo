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
package org.fcrepo.http.commons.responses;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.openrdf.rio.RDFFormat.NO_CONTEXTS;
import static org.openrdf.rio.RDFFormat.NO_NAMESPACES;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.rdf.impl.NamespaceRdfContext;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.ntriples.NTriplesWriterFactory;
import org.slf4j.Logger;

/**
 * Provides serialization for streaming RDF results.
 *
 * @author ajs6f
 * @since Nov 19, 2013
 */
@Provider
@Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X, JSON_LD})
public class RdfStreamProvider implements MessageBodyWriter<RdfStream> {

    private static final Logger LOGGER = getLogger(RdfStreamProvider.class);

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        LOGGER.debug(
                "Checking to see if we can serialize type: {} to mimeType: {}",
                type.getName(), mediaType.toString());
        if (!RdfStream.class.isAssignableFrom(type)) {
            return false;
        }
        if (mediaType.equals(TEXT_HTML_TYPE)
                || mediaType.equals(APPLICATION_XHTML_XML_TYPE)
                || (mediaType.getType().equals("application") && mediaType
                        .getSubtype().equals("html"))) {
            LOGGER.debug("Was asked for an HTML mimeType, returning false.");
            return false;
        }
        LOGGER.debug("Assuming that this is an attempt to retrieve RDF, returning true.");
        return true;
    }

    @Override
    public long getSize(final RdfStream t, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        // We do not know how long the stream is
        return -1;
    }

    @Override
    public void writeTo(final RdfStream rdfStream, final Class<?> type,
        final Type genericType, final Annotation[] annotations,
        final MediaType mediaType,
        final MultivaluedMap<String, Object> httpHeaders,
        final OutputStream entityStream) {

        LOGGER.debug("Serializing an RdfStream to mimeType: {}", mediaType);
        try {
            if (rdfStream.namespaces().isEmpty()) {
                final RdfStream namespaceRdfContext = new NamespaceRdfContext(rdfStream.session());
                rdfStream.namespaces(namespaceRdfContext.namespaces());
            }

            final RdfStreamStreamingOutput streamOutput = new RdfStreamStreamingOutput(rdfStream, mediaType);
            streamOutput.write(entityStream);
        } catch (final RepositoryException e) {
            throw new WebApplicationException(e);
        }


    }

    /**
     * Add the correct mimeType for n-triples.
     */
    @PostConstruct
    public void registerMimeTypes() {
        RDFWriterRegistry.getInstance().add(new NTriplesWithCorrectMimeType());
    }

    /**
     * OpenRDF uses the wrong mimeType for n-triples, so we offer the correct
     * one as well.
     *
     * @author ajs6f
     * @since Nov 20, 2013
     */
    public static class NTriplesWithCorrectMimeType extends
        NTriplesWriterFactory {

        private static final RDFFormat NTRIPLESWITHCORRECTMIMETYPE =
            new RDFFormat("N-Triples-with-correct-mimeType",
                    NTRIPLES, Charset.forName("US-ASCII"), "nt",
                    NO_NAMESPACES, NO_CONTEXTS);

        @Override
        public RDFFormat getRDFFormat() {
            return NTRIPLESWITHCORRECTMIMETYPE;
        }
    }

}
