package org.fcrepo.provider;

import static org.fcrepo.AbstractResource.toPath;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.riot.WebContent;
import org.fcrepo.FedoraObject;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.AuthenticatedSessionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.update.GraphStore;

public class GraphStreamingOutput implements StreamingOutput {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(GraphStreamingOutput.class);
	
	private final AuthenticatedSessionProvider m_sessions;
	private final ObjectService m_objects;
	private final String m_path;
	private final String m_format;
    
	public GraphStreamingOutput(AuthenticatedSessionProvider sessions, ObjectService objects,
			final String path, final MediaType mediaType) {
    	m_sessions = sessions;
    	m_objects = objects;
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

			final FedoraObject object = m_objects.getObject(session, m_path);
			final GraphStore graphStore = object.getGraphStore();

			graphStore.toDataset().getDefaultModel().write(out, m_format);
		} catch (final RepositoryException e) {
			throw new WebApplicationException(e);
		} finally {
			if (session != null) session.logout();
		}

	}

}
