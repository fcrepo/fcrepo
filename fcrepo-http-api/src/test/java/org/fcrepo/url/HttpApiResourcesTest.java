package org.fcrepo.url;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.FedoraNodes;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.serialization.SerializerUtil;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import java.util.HashSet;

import static org.fcrepo.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_NAMESPACE_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_SEARCH_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.RdfLexicon.HAS_SITEMAP;
import static org.fcrepo.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.RdfLexicon.HAS_VERSION_HISTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpApiResourcesTest {

    private HttpApiResources testObj;
    private Node mockNode;
    private FedoraResource mockResource;
    private UriInfo uriInfo;
    private GraphSubjects mockSubjects;
    private SerializerUtil mockSerializers;

    @Before
    public void setUp() {
        testObj = new HttpApiResources();
        mockNode = mock(Node.class);
        mockResource = new FedoraResource(mockNode);

        uriInfo = TestHelpers.getUriInfoImpl();
        mockSubjects = new HttpGraphSubjects(FedoraNodes.class, uriInfo);

        mockSerializers = mock(SerializerUtil.class);
        testObj.setSerializers(mockSerializers);
    }

    @Test
    public void shouldDecorateModeRootNodesWithRepositoryWideLinks() throws RepositoryException {

        final NodeType mockNodeType = mock(NodeType.class);
        when(mockNodeType.isNodeType(FedoraJcrTypes.ROOT)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockNode.getPath()).thenReturn("/");

        Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model = testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_SEARCH_SERVICE));
        assertTrue(model.contains(graphSubject, HAS_SITEMAP));
        assertTrue(model.contains(graphSubject, HAS_TRANSACTION_SERVICE));
        assertTrue(model.contains(graphSubject, HAS_NAMESPACE_SERVICE));
    }

    @Test
    public void shouldDecorateNodesWithLinksToVersionsAndExport() throws RepositoryException {

        when(mockNode.getPrimaryNodeType()).thenReturn(mock(NodeType.class));
        when(mockNode.getPath()).thenReturn("/some/path/to/object");

        when(mockSerializers.keySet()).thenReturn(ImmutableSet.of("a", "b"));
        Resource graphSubject = mockSubjects.getGraphSubject(mockNode);


        final Model model = testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_VERSION_HISTORY));
        assertEquals(2, model.listObjectsOfProperty(graphSubject, HAS_SERIALIZATION).toSet().size());
    }

    @Test
    public void shouldDecorateDatastreamsWithLinksToFixityChecks() throws RepositoryException {
        when(mockNode.hasNode(JcrConstants.JCR_CONTENT)).thenReturn(true);
        when(mockNode.getPrimaryNodeType()).thenReturn(mock(NodeType.class));
        when(mockNode.getPath()).thenReturn("/some/path/to/datastream");
        when(mockSerializers.keySet()).thenReturn(new HashSet<String>());
        Resource graphSubject = mockSubjects.getGraphSubject(mockNode);

        final Model model = testObj.createModelForResource(mockResource, uriInfo, mockSubjects);

        assertTrue(model.contains(graphSubject, HAS_FIXITY_SERVICE));
    }

}
