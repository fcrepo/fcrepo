package org.fcrepo.api;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.tika.io.IOUtils;
import org.jboss.resteasy.specimpl.PathSegmentImpl;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;

public abstract class TestHelpers {
	
	static String MOCK_PREFIX = "mockPrefix";
	static String MOCK_URI_STRING = "mock.namespace.org";
    
    public static UriInfo getUriInfoImpl() {
    	URI baseURI = URI.create("/fcrepo");
    	URI absoluteURI = URI.create("http://localhost/fcrepo");
        URI absolutePath = UriBuilder.fromUri(absoluteURI).replaceQuery(null).build();
        // path must be relative to the application's base uri
  	    URI relativeUri = baseURI.relativize(absoluteURI);
  		
        List<PathSegment> encodedPathSegments = PathSegmentImpl.parseSegments(relativeUri.getRawPath(), false);
        return new UriInfoImpl(absolutePath, baseURI, "/" + relativeUri.getRawPath(), absoluteURI.getRawQuery(), encodedPathSegments);
    }
    
    public static List<Attachment> getStringsAsAttachments(Map<String, String> contents) {
    	List<Attachment> results = new ArrayList<Attachment>(contents.size());
    	for (String id:contents.keySet()) {
    		String content = contents.get(id);
    		InputStream contentStream = IOUtils.toInputStream(content);
    		ContentDisposition cd =
    				new ContentDisposition("form-data;name=" + id + ";filename=" + id + ".txt");
    		Attachment a = new Attachment(id, contentStream, cd);
    		results.add(a);
    	}
    	return results;
    }
    
    @SuppressWarnings("unchecked")
    public static Query getQueryMock() {
		Query mockQ = mock(Query.class);
		QueryResult mockResults = mock(QueryResult.class);
		NodeIterator mockNodes = mock(NodeIterator.class);
		when(mockNodes.getSize()).thenReturn(2L);
		when(mockNodes.hasNext()).thenReturn(true, true, false);
		Node node1 = mock(Node.class);
		Node node2 = mock(Node.class);
		try{
			when(node1.getName()).thenReturn("node1");
			when(node2.getName()).thenReturn("node2");
		} catch (RepositoryException e){
			e.printStackTrace();
		}
		when(mockNodes.nextNode()).thenReturn(node1, node2).thenThrow(IndexOutOfBoundsException.class);
		try {
			when(mockResults.getNodes()).thenReturn(mockNodes);
			when(mockQ.execute()).thenReturn(mockResults);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return mockQ;
    }
    
    public static Session getQuerySessionMock() {
    	Session mock = mock(Session.class);
    	Workspace mockWS = mock(Workspace.class);
    	QueryManager mockQM = mock(QueryManager.class);
    	try {
    		Query mockQ = getQueryMock();
    		when(mockQM.createQuery(anyString(), anyString())).thenReturn(mockQ);
			when(mockWS.getQueryManager()).thenReturn(mockQM);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
    	when(mock.getWorkspace()).thenReturn(mockWS);
    	ValueFactory mockVF = mock(ValueFactory.class);
    	try {
			when(mock.getValueFactory()).thenReturn(mockVF);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
    	return mock;
    }
    
    public static Session getSessionMock() throws RepositoryException {
    	String[] mockPrefixes = { MOCK_PREFIX };
    	Session mockSession = mock(Session.class);
    	Workspace mockWorkspace = mock(Workspace.class);
    	NamespaceRegistry mockNameReg = mock(NamespaceRegistry.class);
    	NodeTypeManager mockNTM = mock(NodeTypeManager.class);
    	NodeTypeIterator mockNTI = mock(NodeTypeIterator.class);
    	NodeType mockNodeType = mock(NodeType.class);
    	when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
    	when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNameReg);
    	when(mockNameReg.getPrefixes()).thenReturn(mockPrefixes);
    	when(mockNameReg.getURI(MOCK_PREFIX)).thenReturn(MOCK_URI_STRING);
    	when(mockWorkspace.getNodeTypeManager()).thenReturn(mockNTM);
    	when(mockNodeType.getName()).thenReturn("mockName");
    	when(mockNodeType.toString()).thenReturn("mockString");
    	when(mockNTM.getAllNodeTypes()).thenReturn(mockNTI);
    	when(mockNTI.hasNext()).thenReturn(true,false);
    	when(mockNTI.nextNodeType()).thenReturn(mockNodeType).thenThrow(ArrayIndexOutOfBoundsException.class);
    	return mockSession;
    }
    
    public static Repository createMockRepo() throws RepositoryException {
    	Repository mockRepo = mock(Repository.class);
    	Session mockSession = getSessionMock();
    	
    	when(mockRepo.getDescriptor("jcr.repository.name")).thenReturn("Mock Repository");
    	String[] mockKeys = { MOCK_PREFIX };
    	
    
    	when(mockRepo.login()).thenReturn(mockSession);
    	when(mockRepo.getDescriptorKeys()).thenReturn(mockKeys);
    	
    	return mockRepo;
    }
    
}
