
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

import org.fcrepo.api.repository.FedoraRepositoryNamespaces;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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
        testObj.setSession(mockSession);
        testObj.setNodeService(mockNodeService);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetNamespaces() throws RepositoryException, IOException {

        final Dataset mockDataset = mock(Dataset.class);

        when(mockNodeService.getNamespaceRegistryGraph(mockSession))
                .thenReturn(mockDataset);
        assertEquals(mockDataset, testObj.getNamespaces());
    }

    @Test
    public void testUpdateNamespaces() throws RepositoryException, IOException {

        final Model model = ModelFactory.createDefaultModel();
        final Dataset mockDataset = DatasetFactory.create(model);

        when(mockNodeService.getNamespaceRegistryGraph(mockSession))
                .thenReturn(mockDataset);

        testObj.updateNamespaces(new ByteArrayInputStream(
                "INSERT { <http://example.com/this> <http://example.com/is> \"abc\"} WHERE { }"
                        .getBytes()));

        assertEquals(1, model.size());
    }
}
