package org.fcrepo.provider;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.WebApplicationException;

import org.fcrepo.FedoraObject;
import org.fcrepo.http.RDFMediaType;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.fcrepo.session.AuthenticatedSessionProvider;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.sparql.modify.GraphStoreNull;
import com.hp.hpl.jena.update.GraphStore;

public class GraphStreamingOutputTest {

	AuthenticatedSessionProvider mockSessions;
	
	Session mockSession;
	
	NodeService mockNodes;
	
	@Before
	public void setUp(){
		mockSessions = mock(AuthenticatedSessionProvider.class);
		mockSession = mock(Session.class);
		when(mockSessions.getAuthenticatedSession()).thenReturn(mockSession);
		mockNodes = mock(NodeService.class);
	}
	
	@Test
	public void testStuff() throws WebApplicationException, IOException, RepositoryException {
		String testPath = "/does/not/exist";
		FedoraObject mockObject = mock(FedoraObject.class);
		when(mockNodes.getObject(mockSession, testPath)).thenReturn(mockObject);
		GraphStore graph = new GraphStoreNull();
		when(mockObject.getGraphStore()).thenReturn(graph);
		GraphStreamingOutput test =
				new GraphStreamingOutput(mockSessions, mockNodes, testPath, RDFMediaType.NTRIPLES_TYPE);
		OutputStream mockOut = mock(OutputStream.class);
		test.write(mockOut);
	}
}
