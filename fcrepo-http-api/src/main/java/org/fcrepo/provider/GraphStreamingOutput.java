package org.fcrepo.provider;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.riot.WebContent;
import org.fcrepo.FedoraResource;
import org.fcrepo.services.NodeService;
import org.fcrepo.session.AuthenticatedSessionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.update.GraphStore;

public class GraphStreamingOutput implements StreamingOutput {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(GraphStreamingOutput.class);

	private final GraphStore m_graphStore;
	private final String m_format;
    
	public GraphStreamingOutput(final GraphStore graphStore, final MediaType mediaType) {
        m_graphStore = graphStore;
		m_format = WebContent.contentTypeToLang(mediaType.toString()).getName().toUpperCase();
    }
    
	@Override
	public void write(OutputStream out) throws IOException,
			WebApplicationException {

		m_graphStore.toDataset().getDefaultModel().write(out, m_format);

	}

}
