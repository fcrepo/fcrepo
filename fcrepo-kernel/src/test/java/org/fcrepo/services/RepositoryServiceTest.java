
package org.fcrepo.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

import com.codahale.metrics.MetricRegistry;

public class RepositoryServiceTest implements FedoraJcrTypes {

    String testPid = "testObj";

    String testDsId = "testDs";

    String mockPrefix = "mock1";

    RepositoryService testObj;

    MetricRegistry mockMetricRegistry;

    Repository mockRepo;

    PrintStream mockPrintStream;

    Session mockSession;

    Node mockRootNode;

    Node mockDsNode;

    Long expectedSize = 5L;

    NodeTypeIterator mockNTI;

    Map<String, String> expectedNS;

    @Before
    public void setUp() {
        final String relPath = "/" + testPid + "/" + testDsId;
        final String[] mockPrefixes = {mockPrefix};
        expectedNS = new HashMap<String, String>();

        mockSession = mock(Session.class);
        mockRootNode = mock(Node.class);
        mockDsNode = mock(Node.class);
        mockRepo = mock(Repository.class);
        final NodeIterator mockNI = mock(NodeIterator.class);
        final Property mockProp = mock(Property.class);

        try {
            when(mockSession.getRootNode()).thenReturn(mockRootNode);
            when(mockRootNode.getNode(relPath)).thenReturn(mockDsNode);
            when(mockRootNode.getProperty("fedora:size")).thenReturn(mockProp);
            when(mockProp.getLong()).thenReturn(expectedSize);
            when(mockRepo.login()).thenReturn(mockSession);

            final Workspace mockWorkspace = mock(Workspace.class);
            final QueryManager mockQueryManager = mock(QueryManager.class);
            final QueryResult mockQueryResult = mock(QueryResult.class);
            final Query mockQuery = mock(Query.class);
            final RowIterator mockRI = mock(RowIterator.class);
            final Value mockValue = mock(Value.class);
            final Row mockRow = mock(Row.class);
            final NodeTypeManager mockNodeTypeManager =
                    mock(NodeTypeManager.class);
            final NamespaceRegistry mockNamespaceRegistry =
                    mock(NamespaceRegistry.class);

            mockNTI = mock(NodeTypeIterator.class);
            testObj = new RepositoryService();
            testObj.setRepository(mockRepo);

            when(mockSession.getNode("/objects")).thenReturn(mockRootNode);
            when(mockRootNode.getNodes()).thenReturn(mockNI);
            when(mockNI.getSize()).thenReturn(expectedSize);
            when(testObj.findOrCreateNode(mockSession, "/objects")).thenReturn(
                    mockRootNode);
            when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
            when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
            when(mockWorkspace.getNodeTypeManager()).thenReturn(
                    mockNodeTypeManager);
            when(mockWorkspace.getNamespaceRegistry()).thenReturn(
                    mockNamespaceRegistry);
            when(mockNamespaceRegistry.getPrefixes()).thenReturn(mockPrefixes);
            when(mockNodeTypeManager.getAllNodeTypes()).thenReturn(mockNTI);
            when(mockQueryManager.createQuery(anyString(), eq(Query.JCR_SQL2)))
                    .thenReturn(mockQuery);
            when(mockQuery.execute()).thenReturn(mockQueryResult);
            when(mockQueryResult.getRows()).thenReturn(mockRI);
            when(mockRI.hasNext()).thenReturn(true, false);
            when(mockRI.nextRow()).thenReturn(mockRow);
            when(mockRow.getValue(CONTENT_SIZE)).thenReturn(mockValue);
            when(mockValue.getLong()).thenReturn(expectedSize);

            expectedNS
                    .put(mockPrefix, mockNamespaceRegistry.getURI(mockPrefix));

        } catch (final RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testDumpMetrics() {
        RepositoryService.dumpMetrics(mockPrintStream);
    }

    @Test
    public void testGetRepositorySize() throws RepositoryException {
        final Long actual = testObj.getRepositorySize();
        assertEquals(expectedSize, actual);
    }

    @Test
    public void testGetRepositoryObjectCount() {
        final Long actual = testObj.getRepositoryObjectCount(mockSession);
        assertEquals(expectedSize, actual);
    }

    @Test
    public void testGetAllNodeTypes() throws RepositoryException {
        final NodeTypeIterator actual = testObj.getAllNodeTypes(mockSession);
        assertEquals(mockNTI, actual);
    }

    @Test
    public void testGetRepositoryNamespaces() throws RepositoryException {
        final Map<String, String> actual =
                testObj.getRepositoryNamespaces(mockSession);
        assertEquals(expectedNS, actual);
    }
    
    @Test
    public void testExists() throws RepositoryException {
        String existsPath = "/foo/bar/exists";
        when(mockSession.nodeExists(existsPath)).thenReturn(true);
        assertEquals(true, testObj.exists(mockSession, existsPath));
        assertEquals(false, testObj.exists(mockSession, "/foo/bar"));
    }

    @Test
    public void testIsFile() throws RepositoryException {
        String filePath = "/foo/bar/file";
        String folderPath = "/foo/bar/folder";
        Node mockFile = mock(Node.class);
        when(mockFile.isNodeType("nt:file")).thenReturn(true);
        Node mockFolder = mock(Node.class);
        when(mockFolder.isNodeType("nt:file")).thenReturn(false);
        when(mockSession.getNode(filePath)).thenReturn(mockFile);
        when(mockSession.getNode(folderPath)).thenReturn(mockFolder);
        assertEquals(true, testObj.isFile(mockSession, filePath));
        assertEquals(false, testObj.isFile(mockSession, folderPath));
    }
}
