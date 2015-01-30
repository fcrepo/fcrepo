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

import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static org.openrdf.model.impl.ValueFactoryImpl.getInstance;
import static org.openrdf.model.util.Literals.createLiteral;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
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

    private final RDFFormat format;

    private final RdfStream rdfStream;

    /**
     * Normal constructor
     *
     * @param rdfStream
     * @param mediaType
     */
    public RdfStreamStreamingOutput(final RdfStream rdfStream,
            final MediaType mediaType) {
        super();

        if (LOGGER.isDebugEnabled()) {
            for (final RDFFormat writeableFormats : RDFWriterRegistry.getInstance().getKeys()) {
                LOGGER.debug("Discovered RDF writer writeableFormats: {} with mimeTypes: {}",
                        writeableFormats.getName(), Joiner.on(" ")
                                .join(writeableFormats.getMIMETypes()));
            }
        }
        final RDFFormat format = Rio.getWriterFormatForMIMEType(mediaType.toString());
        if (format != null) {
            this.format = format;
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
            write(asStatements(), output, format);
        } catch (final RDFHandlerException e) {
            setException(e);
            LOGGER.debug("Error serializing RDF", e);
            throw new WebApplicationException(e);
        }
    }

    private void write(final Iterable<Statement> model,
                       final OutputStream output,
                       final RDFFormat dataFormat)
            throws RDFHandlerException {
        final WriterConfig settings = new WriterConfig();
        final RDFWriter writer = Rio.createWriter(dataFormat, output);
        writer.setWriterConfig(settings);

        for (final Map.Entry<String, String> namespace : excludeProtectedNamespaces(rdfStream.namespaces())) {
            writer.handleNamespace(namespace.getKey(), namespace.getValue());
        }

        Rio.write(model, writer);
    }

    private static Iterable<Map.Entry<String, String>> excludeProtectedNamespaces(
            final Map<String, String> namespaces) {
        /**
         * We exclude:
         *  - xmlns, which Sesame helpfully serializes, but normal parsers may complain
         *     about in some serializations (e.g. RDF/XML where xmlns:xmlns is forbidden by XML);
         */
        return Iterables.filter(namespaces.entrySet(), new Predicate<Map.Entry<String, String>>() {
            @Override
            public boolean apply(final Map.Entry<String, String> input) {
                return !input.getKey().equals("xmlns");
            }
        });
    }


    private Iterable<Statement> asStatements() {
        return new Iterable<Statement>() {

            @Override
            public Iterator<Statement> iterator() {
                return rdfStream.transform(toStatement);
            }
        };
    }

    protected static final Function<? super Triple, Statement> toStatement =
        new Function<Triple, Statement>() {

            @Override
            public Statement apply(final Triple t) {
                final Resource subject = getResourceForSubject(t.getSubject());
                final URI predicate = vfactory.createURI(t.getPredicate().getURI());
                final Value object = getValueForObject(t.getObject());
                return vfactory.createStatement(subject, predicate, object);
            }

        };

    private static Resource getResourceForSubject(final Node subjectNode) {
        final Resource subject;

        if (subjectNode.isBlank()) {
            subject = vfactory.createBNode(subjectNode.getBlankNodeLabel());
        } else {
            subject = vfactory.createURI(subjectNode.getURI());
        }

        return subject;
    }

    protected static Value getValueForObject(final Node object) {
        final Value value;
        if (object.isURI()) {
            value = vfactory.createURI(object.getURI());
        } else if (object.isBlank()) {
            value = vfactory.createBNode(object.getBlankNodeLabel());
        } else if (object.isLiteral()) {
            final String literalValue = object.getLiteralLexicalForm();

            final String literalDatatypeURI = object.getLiteralDatatypeURI();

            if (!object.getLiteralLanguage().isEmpty()) {
                value = vfactory.createLiteral(literalValue, object.getLiteralLanguage());
            } else if (literalDatatypeURI != null) {
                final URI uri = vfactory.createURI(literalDatatypeURI);
                value = vfactory.createLiteral(literalValue, uri);
            } else {
                value = createLiteral(vfactory, object.getLiteralValue());
            }
        } else {
            // should not happen..
            throw new AssertionError("Unable to convert " + object + " to a value");
        }

        return value;
    }
}
