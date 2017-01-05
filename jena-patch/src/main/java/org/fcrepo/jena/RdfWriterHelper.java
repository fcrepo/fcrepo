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
package org.fcrepo.jena;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.CharSpace;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.WriterGraphRIOTFactory;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.riot.system.StreamRDFWriterFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author awoods
 * @since 2017/01/03
 */
@Component
public class RdfWriterHelper {

    private static final Logger LOGGER = getLogger(RdfWriterHelper.class);

    private RdfWriterHelper() {
        // prevent construction
    }

    private static StreamRDFWriterFactory streamWriterFactoryBlocks =
            (output, format) -> new FedoraWriterStreamRDFBlocks(output);

    private static StreamRDFWriterFactory streamWriterFactoryFlat =
            (output, format) -> new FedoraWriterStreamRDFFlat(output);

    private static StreamRDFWriterFactory streamWriterFactoryTriplesQuads =
            (output, format) -> {
                final AWriter w = IO.wrapUTF8(output);
                return new FedoraWriterStreamRDFPlain(w, CharSpace.UTF8);     // N-Quads and N-Triples.
            };

    private static StreamRDFWriterFactory streamWriterFactoryTriplesQuadsAscii =
            (output, format) -> {
                final AWriter w = IO.wrapUTF8(output);
                return new FedoraWriterStreamRDFPlain(w, CharSpace.ASCII);     // N-Quads and N-Triples.
            };

    private static WriterGraphRIOTFactory wgfactory = (serialization) ->
        {
            if ( Objects.equals(RDFFormat.TURTLE_PRETTY, serialization) ) {
                return new FedoraTurtleWriter();
            }
            if ( Objects.equals(RDFFormat.TURTLE_BLOCKS, serialization) ) {
                return new FedoraTurtleWriterBlocks();
            }
            if ( Objects.equals(RDFFormat.TURTLE_FLAT, serialization) ) {
                return new FedoraTurtleWriterFlat();
            }
            if ( Objects.equals(RDFFormat.RDFXML_PLAIN, serialization) ) {
                return new FedoraRDFXMLPlainWriter();
            }

            return null ;
        };


    static {
        LOGGER.info("Loading JENA 3.1.1 Patched RDF Output Writers");
        StreamRDFWriter.register(RDFFormat.TURTLE_BLOCKS, streamWriterFactoryBlocks);
        StreamRDFWriter.register(RDFFormat.TURTLE_FLAT, streamWriterFactoryFlat);

        StreamRDFWriter.register(RDFFormat.NTRIPLES, streamWriterFactoryTriplesQuads);
        StreamRDFWriter.register(RDFFormat.NTRIPLES_UTF8, streamWriterFactoryTriplesQuads);
        StreamRDFWriter.register(RDFFormat.NTRIPLES_ASCII, streamWriterFactoryTriplesQuadsAscii);

        RDFWriterRegistry.register(RDFFormat.TURTLE_PRETTY,  wgfactory) ;
        RDFWriterRegistry.register(RDFFormat.TURTLE_BLOCKS,  wgfactory) ;
        RDFWriterRegistry.register(RDFFormat.TURTLE_FLAT,    wgfactory) ;

        RDFWriterRegistry.register(RDFFormat.RDFXML_PLAIN,   wgfactory) ;
    }
}
