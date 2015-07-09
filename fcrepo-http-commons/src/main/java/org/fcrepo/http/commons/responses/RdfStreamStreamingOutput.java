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

import static com.google.common.collect.Maps.filterEntries;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static org.fcrepo.kernel.utils.UncheckedBiConsumer.uncheck;
import static org.openrdf.model.impl.ValueFactoryImpl.getInstance;
import static org.openrdf.model.util.Literals.createLiteral;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.OutputStream;
import java.util.function.Function;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.kernel.utils.iterators.RdfStream;

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
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

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

    /**
     * Normal constructor
     *
     * @param rdfStream the rdf stream
     * @param mediaType the media type
     */
    public RdfStreamStreamingOutput(final RdfStream rdfStream,
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
    }

    @Override
    public void write(final OutputStream output) {
        LOGGER.debug("Serializing RDF stream in: {}", format);
        try {
            write(() -> rdfStream.transform(toStatement::apply), output, format, mediaType);
        } catch (final RDFHandlerException e) {
            setException(e);
            LOGGER.debug("Error serializing RDF", e);
            throw new WebApplicationException(e);
        }
    }

    private void write(final Iterable<Statement> model,
                       final OutputStream output,
                       final RDFFormat dataFormat,
                       final MediaType dataMediaType)
            throws RDFHandlerException {
        final WriterConfig settings = WriterConfigHelper.apply(dataMediaType);
        final RDFWriter writer = Rio.createWriter(dataFormat, output);
        writer.setWriterConfig(settings);

        /**
         * We exclude:
         *  - xmlns, which Sesame helpfully serializes, but normal parsers may complain
         *     about in some serializations (e.g. RDF/XML where xmlns:xmlns is forbidden by XML);
         */
        filterEntries(rdfStream.namespaces(), e -> !e.getKey().equals("xmlns"))
                .forEach(uncheck(writer::handleNamespace));

        Rio.write(model, writer);
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
