
package org.fcrepo.api.repository;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraObject;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraRepositoriesPropertiesTest {

    private FedoraRepositoriesProperties testObj;

    private NodeService mockNodes;

    private Session mockSession;

    @Before
    public void setUp() throws RepositoryException {
        mockNodes = mock(NodeService.class);
        testObj = new FedoraRepositoriesProperties();
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setSession(mockSession);
        testObj.setNodeService(mockNodes);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @Test
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final FedoraObject mockObject = mock(FedoraObject.class);

        when(mockObject.getDatasetProblems()).thenReturn(null);
        final InputStream mockStream =
                new ByteArrayInputStream("my-sparql-statement".getBytes());
        when(mockNodes.getObject(mockSession, "/")).thenReturn(mockObject);

        testObj.updateSparql(mockStream);

        verify(mockObject).updatePropertiesDataset(any(GraphSubjects.class),
                eq("my-sparql-statement"));
        verify(mockSession).save();
        verify(mockSession).logout();
    }
}
