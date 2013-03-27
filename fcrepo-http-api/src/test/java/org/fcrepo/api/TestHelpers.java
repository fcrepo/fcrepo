package org.fcrepo.api;

import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
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
import org.modeshape.jcr.query.QueryResults;

public abstract class TestHelpers {
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
    
}
