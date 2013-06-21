
package org.fcrepo.responses;

import static org.apache.jena.riot.WebContent.contentTypeToLang;
import static org.fcrepo.responses.RdfSerializationUtils.unifyDatasetModel;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStore;

public class GraphStoreStreamingOutput implements StreamingOutput {

    private static final Logger LOGGER =
            getLogger(GraphStoreStreamingOutput.class);

    private final Dataset dataset;

    private final String format;


    public GraphStoreStreamingOutput(final GraphStore graphStore,
                                     final MediaType mediaType) {
        this(graphStore.toDataset(), mediaType);
    }

    public GraphStoreStreamingOutput(final Dataset dataset,
            final MediaType mediaType) {
        this.dataset = dataset;
        format =
                contentTypeToLang(mediaType.toString()).getName().toUpperCase();
    }

    @Override
    public void write(final OutputStream out) throws IOException,
            WebApplicationException {
        LOGGER.debug("Serializing graph  as {}", format);
        LOGGER.debug("Serializing default model");
        Model model = unifyDatasetModel(dataset);

        model.write(out, format);
    }

}
