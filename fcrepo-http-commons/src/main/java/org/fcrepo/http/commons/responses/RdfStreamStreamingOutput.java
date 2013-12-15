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

package org.fcrepo.http.commons.responses;

import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static org.openrdf.model.impl.ValueFactoryImpl.getInstance;
import static org.openrdf.model.util.Literals.createLiteral;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;
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
 * @date Oct 30, 2013
 */
public class RdfStreamStreamingOutput extends AbstractFuture<Void> implements
        StreamingOutput {

    private static final Logger LOGGER =
        getLogger(RdfStreamStreamingOutput.class);

    private static ValueFactory vfactory = getInstance();

    private final RDFFormat format;

    private final RdfStream rdfStream;

    private static final Void finishedMarker = null;

    /**
     * Normal constructor
     *
     * @param rdfStream
     * @param mediaType
     */
    public RdfStreamStreamingOutput(final RdfStream rdfStream,
            final MediaType mediaType) {
        super();
        for (final RDFFormat format : RDFWriterRegistry.getInstance().getKeys()) {
            LOGGER.debug("Discovered RDF writer format: {} with mimeTypes: {}",
                    format.getName(), Joiner.on(" ")
                            .join(format.getMIMETypes()));
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
    public void write(final OutputStream output) throws IOException {
        LOGGER.debug("Serializing RDF stream in: {}", format);
        try {
            Rio.write(asStatements(), output, format);
            set(finishedMarker);
        } catch (final RDFHandlerException e) {
            setException(e);
            throw new WebApplicationException(e);
        }
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
                final Value object = getValueForObject(t.getObject());
                return vfactory.createStatement(vfactory.createURI(t
                        .getSubject().getURI()), vfactory.createURI(t
                        .getPredicate().getURI()), object);
            }

        };

    protected static Value getValueForObject(final Node object) {
        if (object.isURI()) {
            return vfactory.createURI(object.getURI());
        } else {
            if (object.isLiteral()) {
                return createLiteral(vfactory, object.getLiteralValue());
            }
        }
        throw new UnsupportedOperationException(
                "We do not serialize blank nodes!");
    }
}
