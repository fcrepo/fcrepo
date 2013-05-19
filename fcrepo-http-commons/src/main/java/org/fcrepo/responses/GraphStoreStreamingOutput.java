
package org.fcrepo.responses;

import static org.apache.jena.riot.WebContent.contentTypeToLang;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;

import com.hp.hpl.jena.update.GraphStore;

public class GraphStoreStreamingOutput implements StreamingOutput {

    private final static Logger LOGGER =
            getLogger(GraphStoreStreamingOutput.class);

    private final GraphStore m_graphStore;

    private final String m_format;

    public GraphStoreStreamingOutput(final GraphStore graphStore,
            final MediaType mediaType) {
        m_graphStore = graphStore;
        m_format =
                contentTypeToLang(mediaType.toString()).getName().toUpperCase();
    }

    @Override
    public void write(final OutputStream out) throws IOException,
            WebApplicationException {
        LOGGER.debug("Writing to: {}", out.hashCode());
        m_graphStore.toDataset().getDefaultModel().write(out, m_format);

    }

}
