package org.fcrepo.api;

import static org.fcrepo.TestHelpers.mockDatastreamNode;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.services.DatastreamService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraDatastreamsTest {
	
	FedoraDatastreams testObj;
	
	DatastreamService mockDatastreams;
	
	@Before
	public void setUp() {
		mockDatastreams = mock(DatastreamService.class);
		testObj = new FedoraDatastreams();
		testObj.datastreamService = mockDatastreams;
	}
	
	@After
	public void tearDown() {
		
	}
	
    @Test
	public void testGetDatastreams() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsid = "testDS";
    	NodeIterator mockIter = mockDatastreamNode(dsid, "asdf");
    	when(mockDatastreams.getDatastreamsFor(pid)).thenReturn(mockIter);
    	ObjectDatastreams actual = testObj.getDatastreams(pid);
    	verify(mockDatastreams).getDatastreamsFor(pid);
    	assertEquals(1, actual.datastreams.size());
    	assertEquals(dsid, actual.datastreams.iterator().next().dsid);
    }
}
