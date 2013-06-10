
package org.fcrepo.responses;

import static org.apache.jena.riot.WebContent.contentTypeToLang;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStore;
import org.slf4j.Logger;

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
        final Iterator<String> iterator = dataset.listNames();
        LOGGER.debug("Serializing default model");
        Model model = dataset.getDefaultModel();
        while (iterator.hasNext()) {
            final String modelName = iterator.next();
            LOGGER.debug("Serializing model {}", modelName);
            model = model.union(dataset.getNamedModel(modelName));
        }
        model.write(out, format);
    }

}
