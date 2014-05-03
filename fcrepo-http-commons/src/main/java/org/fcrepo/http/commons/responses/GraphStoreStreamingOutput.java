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

import static org.apache.jena.riot.WebContent.contentTypeToLang;
import static org.fcrepo.kernel.rdf.SerializationUtils.unifyDatasetModel;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStore;

/**
 * Helper for serializing a GraphStore or Dataset to a RDF
 * serialization format (given by the serialization's mime type)
 *
 * @author awoods
 */
public class GraphStoreStreamingOutput implements StreamingOutput {

    private static final Logger LOGGER =
        getLogger(GraphStoreStreamingOutput.class);

    private final Dataset dataset;

    private final String format;

    /**
     * Construct the StreamingOutput machinery to serialize
     * a GraphStore to the mime type given
     * @param graphStore
     * @param mediaType
     */
    public GraphStoreStreamingOutput(final GraphStore graphStore,
            final MediaType mediaType) {
        this(graphStore.toDataset(), mediaType);
    }

    /**
     * Construct the StreamingOutput machinery to serialize
     * a Dataset to the mime type given
     * @param dataset
     * @param mediaType
     */
    public GraphStoreStreamingOutput(final Dataset dataset,
            final MediaType mediaType) {
        this.dataset = dataset;
        this.format =
            contentTypeToLang(mediaType.toString()).getName().toUpperCase();
    }

    /**
     * Construct the StreamingOutput machinery to serialize
     * an RdfStream to the mime type given
     * @param stream
     * @param mediaType
     */
    public GraphStoreStreamingOutput(final RdfStream stream,
            final MediaType mediaType) {
        this.dataset = DatasetFactory.create(stream.asModel());
        this.format =
            contentTypeToLang(mediaType.toString()).getName().toUpperCase();
    }

    @Override
    public void write(final OutputStream out) throws IOException {
        LOGGER.debug("Serializing graph  as {}", format);
        LOGGER.debug("Serializing default model");
        final Model model = unifyDatasetModel(dataset);

        model.write(out, format);
    }

}
