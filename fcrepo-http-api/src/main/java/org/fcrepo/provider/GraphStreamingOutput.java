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
	
	private final AuthenticatedSessionProvider m_sessions;
	private final NodeService m_nodes;
	private final String m_path;
	private final String m_format;
    
	public GraphStreamingOutput(AuthenticatedSessionProvider sessions, NodeService objects,
			final String path, final MediaType mediaType) {
    	m_sessions = sessions;
    	m_nodes = objects;
		LOGGER.trace("getting profile for {}", path);
		m_path = path;
		m_format = WebContent.contentTypeToLang(mediaType.toString()).getName().toUpperCase();
    }
    
	@Override
	public void write(OutputStream out) throws IOException,
			WebApplicationException {
		Session session = null;
		try {
			session = m_sessions.getAuthenticatedSession();

			final FedoraResource resource = m_nodes.getObject(session, m_path);
			final GraphStore graphStore = resource.getGraphStore();

			graphStore.toDataset().getDefaultModel().write(out, m_format);
		} catch (final RepositoryException e) {
			throw new WebApplicationException(e);
		} finally {
			if (session != null) session.logout();
		}

	}

}
