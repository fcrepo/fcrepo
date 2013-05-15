package org.fcrepo.provider;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.WebApplicationException;

import com.hp.hpl.jena.rdf.model.Model;
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

	@Test
	public void testStuff() throws WebApplicationException, IOException, RepositoryException {
		GraphStore graph = new GraphStoreNull();

        final Model model = spy(graph.toDataset().getDefaultModel());

		GraphStreamingOutput test =
				new GraphStreamingOutput(graph, RDFMediaType.NTRIPLES_TYPE);
		OutputStream mockOut = mock(OutputStream.class);
		test.write(mockOut);
        verify(model.write(mockOut, "N-TRIPLES"));
	}
}
