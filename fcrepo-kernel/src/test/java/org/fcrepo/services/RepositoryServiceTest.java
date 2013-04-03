package org.fcrepo.services;

import static org.fcrepo.TestHelpers.getContentNodeMock;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
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

import com.yammer.metrics.MetricRegistry;

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
		String relPath = getDatastreamJcrNodePath(testPid, testDsId).substring(1);
		String[] mockPrefixes = { mockPrefix };
		expectedNS = new HashMap<String, String>();
		
	    mockSession = mock(Session.class);
		mockRootNode = mock(Node.class);
		mockDsNode = mock(Node.class);
		mockRepo = mock(Repository.class);
		NodeIterator mockNI = mock(NodeIterator.class);
		Property mockProp = mock(Property.class);
		
		try{
		    when(mockSession.getRootNode()).thenReturn(mockRootNode);
		    when(mockRootNode.getNode(relPath)).thenReturn(mockDsNode);
		    when(mockRootNode.getProperty("fedora:size")).thenReturn(mockProp);
		    when(mockProp.getLong()).thenReturn(expectedSize);
		    when(mockRepo.login()).thenReturn(mockSession);
		    
			Workspace mockWorkspace = mock(Workspace.class);
			QueryManager mockQueryManager = mock(QueryManager.class);
			QueryResult mockQueryResult = mock(QueryResult.class);
			Query mockQuery = mock(Query.class);
			RowIterator mockRI = mock(RowIterator.class);
			Value mockValue = mock(Value.class);
			Row mockRow = mock(Row.class);
			NodeTypeManager mockNodeTypeManager = mock(NodeTypeManager.class);
			NamespaceRegistry mockNamespaceRegistry = mock(NamespaceRegistry.class);
			
			mockNTI = mock(NodeTypeIterator.class);
			testObj = new RepositoryService();
			testObj.setRepository(mockRepo);
			testObj.readOnlySession = mockSession;
			
			when(mockSession.getNode("/objects")).thenReturn(mockRootNode);
			when(mockRootNode.getNodes()).thenReturn(mockNI);
			when(mockNI.getSize()).thenReturn(expectedSize);
			when(testObj.findOrCreateNode(mockSession, "/objects")).thenReturn(mockRootNode);
			when(testObj.readOnlySession.getWorkspace()).thenReturn(mockWorkspace);
			when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
			when(mockWorkspace.getQueryManager()).thenReturn(mockQueryManager);
			when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNodeTypeManager);
			when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
			when(mockNamespaceRegistry.getPrefixes()).thenReturn(mockPrefixes);
			when(mockNodeTypeManager.getAllNodeTypes()).thenReturn(mockNTI);
			when(mockQueryManager.createQuery(anyString(), eq(Query.JCR_SQL2))).thenReturn(mockQuery);
			when(mockQuery.execute()).thenReturn(mockQueryResult);
			when(mockQueryResult.getRows()).thenReturn(mockRI);
			when(mockRI.hasNext()).thenReturn(true,false);
			when(mockRI.nextRow()).thenReturn(mockRow);
			when(mockRow.getValue(FEDORA_SIZE)).thenReturn(mockValue);
			when(mockValue.getLong()).thenReturn(expectedSize);
			
			expectedNS.put(mockPrefix, mockNamespaceRegistry.getURI(mockPrefix));
			
			
		} catch(RepositoryException e) {
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
	public void testUpdateRepositorySize() throws PathNotFoundException, RepositoryException {
		String content = "asdf";
		Node mockContent = getContentNodeMock(content);
		when(mockDsNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
		testObj.updateRepositorySize(expectedSize, mockSession);
		assertEquals(expectedSize, testObj.getRepositorySize(mockSession));
	}

	@Test
	public void testGetRepositorySize() throws RepositoryException {
		Long actual = testObj.getRepositorySize(mockSession);
		assertEquals(expectedSize, actual);
	}
	
	@Test
	public void testGetRepositoryObjectCount() {
		Long actual = testObj.getRepositoryObjectCount(mockSession);
		assertEquals(expectedSize, actual);
	}
	
	@Test
	public void testGetAllNodeTypes() throws RepositoryException {
		NodeTypeIterator actual = testObj.getAllNodeTypes(mockSession);
		assertEquals(mockNTI, actual);
	}
	
	@Test
	public void testGetRepositoryNamespaces() throws RepositoryException {
		Map<String, String> actual = testObj.getRepositoryNamespaces(mockSession);
		assertEquals(expectedNS, actual);
	}
}