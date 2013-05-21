
package org.fcrepo.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import org.fcrepo.api.repository.FedoraRepositoryNamespaces;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraRepositoryNamespacesTest {

    FedoraRepositoryNamespaces testObj;

    NodeService mockNodeService;
    private Session mockSession;

    @Before
    public void setUp() throws LoginException, RepositoryException,
            URISyntaxException {
        mockNodeService = mock(NodeService.class);

        testObj = new FedoraRepositoryNamespaces();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setNodeService(mockNodeService);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetNamespaces() throws RepositoryException, IOException {

        GraphStore mockGraphStore = mock(GraphStore.class);
        Dataset mockDataset = mock(Dataset.class);
        when(mockGraphStore.toDataset()).thenReturn(mockDataset);

        when(mockNodeService.getNamespaceRegistryGraph(mockSession)).thenReturn(mockGraphStore);
        assertEquals(mockDataset, testObj.getNamespaces());
    }

    @Test
    public void testUpdateNamespaces() throws RepositoryException, IOException {

        final Model model = ModelFactory.createDefaultModel();
        GraphStore mockGraphStore = GraphStoreFactory.create(model);

        when(mockNodeService.getNamespaceRegistryGraph(mockSession)).thenReturn(mockGraphStore);

        testObj.updateNamespaces(new ByteArrayInputStream("INSERT { <http://example.com/this> <http://example.com/is> \"abc\"} WHERE { }".getBytes()));

        assertEquals(1, model.size());
    }
}
