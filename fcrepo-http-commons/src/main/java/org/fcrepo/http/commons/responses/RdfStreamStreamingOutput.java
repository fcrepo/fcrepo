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
import static org.openrdf.model.impl.ValueFactoryImpl.getInstance;
import static org.openrdf.model.util.Literals.createLiteral;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.OutputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.RdfStream;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.WriterConfig;
import org.slf4j.Logger;

import com.google.common.util.concurrent.AbstractFuture;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * Serializes an {@link RdfStream}.
 *
 * @author ajs6f
 * @since Oct 30, 2013
 */
public class RdfStreamStreamingOutput extends AbstractFuture<Void> implements
        StreamingOutput {

    private static final Logger LOGGER =
        getLogger(RdfStreamStreamingOutput.class);

    private static ValueFactory vfactory = getInstance();

    /**
     * This field is used to determine the correct {@link org.openrdf.rio.RDFWriter} created for the
     * {@link javax.ws.rs.core.StreamingOutput}.
     */
    private final RDFFormat format;

    /**
     * This field is used to determine the {@link org.openrdf.rio.WriterConfig} details used by the created
     * {@link org.openrdf.rio.RDFWriter}.
     */
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
            for (final RDFFormat writeableFormats : RDFWriterRegistry.getInstance().getKeys()) {
                LOGGER.debug("Discovered RDF writer writeableFormats: {} with mimeTypes: {}",
                        writeableFormats.getName(), String.join(" ", writeableFormats.getMIMETypes()));
            }
        }
        final RDFFormat format = Rio.getWriterFormatForMIMEType(mediaType.toString());
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
        LOGGER.debug("Serializing RDF stream in: {}", format);
        try {
            write(rdfStream.map(toStatement), output, format, mediaType, namespaces);
        } catch (final RDFHandlerException e) {
            setException(e);
            LOGGER.debug("Error serializing RDF", e);
            throw new WebApplicationException(e);
        }
    }

    private static void write(final Stream<Statement> model,
                       final OutputStream output,
                       final RDFFormat dataFormat,
                       final MediaType dataMediaType,
                       final Map<String, String> nsPrefixes)
            throws RDFHandlerException {
        final WriterConfig settings = WriterConfigHelper.apply(dataMediaType);
        final RDFWriter writer = Rio.createWriter(dataFormat, output);
        writer.setWriterConfig(settings);

        /**
         * We exclude:
         *  - xmlns, which Sesame helpfully serializes, but normal parsers may complain
         *     about in some serializations (e.g. RDF/XML where xmlns:xmlns is forbidden by XML);
         */
        nsPrefixes.entrySet().stream().filter(e -> !e.getKey().equals("xmlns"))
                .forEach(x -> {
                    try {
                        writer.handleNamespace(x.getKey(), x.getValue());
                    } catch (final RDFHandlerException e) {
                        throw new RepositoryRuntimeException(e);
                    }
                });

        Rio.write((Iterable<Statement>)model::iterator, writer);
    }

    protected static final Function<? super Triple, Statement> toStatement = t -> {
        final Resource subject = getResourceForSubject(t.getSubject());
        final URI predicate = vfactory.createURI(t.getPredicate().getURI());
        final Value object = getValueForObject(t.getObject());
        return vfactory.createStatement(subject, predicate, object);
    };

    private static Resource getResourceForSubject(final Node subjectNode) {
        return subjectNode.isBlank() ? vfactory.createBNode(subjectNode.getBlankNodeLabel())
                : vfactory.createURI(subjectNode.getURI());
    }

    protected static Value getValueForObject(final Node object) {
        if (object.isURI()) {
            return vfactory.createURI(object.getURI());
        } else if (object.isBlank()) {
            return vfactory.createBNode(object.getBlankNodeLabel());
        } else if (object.isLiteral()) {
            final String literalValue = object.getLiteralLexicalForm();

            final String literalDatatypeURI = object.getLiteralDatatypeURI();

            if (!object.getLiteralLanguage().isEmpty()) {
                return vfactory.createLiteral(literalValue, object.getLiteralLanguage());
            } else if (literalDatatypeURI != null) {
                final URI uri = vfactory.createURI(literalDatatypeURI);
                return vfactory.createLiteral(literalValue, uri);
            } else {
                return createLiteral(vfactory, object.getLiteralValue());
            }
        }
        throw new AssertionError("Unable to convert " + object +
                " to a value, it is neither URI, blank, nor literal!");
    }
}
