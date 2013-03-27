package org.fcrepo.api;

import static org.fcrepo.TestHelpers.mockDatastream;
import static org.fcrepo.TestHelpers.mockDatastreamIterator;
import static org.fcrepo.api.TestHelpers.getStringsAsAttachments;
import static org.fcrepo.api.TestHelpers.getUriInfoImpl;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.tika.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.jaxb.responses.management.DatastreamHistory;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.utils.DatastreamIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

public class FedoraDatastreamsTest {
	
	FedoraDatastreams testObj;
	
	DatastreamService mockDatastreams;
	
	LowLevelStorageService mockLow;
	
	Repository mockRepo;
	
	Session mockSession;
	
	@Before
	public void setUp() throws LoginException, RepositoryException {
		mockDatastreams = mock(DatastreamService.class);
		mockLow = mock(LowLevelStorageService.class);
		testObj = new FedoraDatastreams();
		testObj.datastreamService = mockDatastreams;
		testObj.llStoreService = mockLow;
		mockRepo = mock(Repository.class);
		mockSession = mock(Session.class);
		when(mockRepo.login()).thenReturn(mockSession);
		testObj.setRepository(mockRepo);
		testObj.setUriInfo(getUriInfoImpl());
	}
	
	@After
	public void tearDown() {
		
	}
	
    @Test
	public void testGetDatastreams() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsid = "testDS";
    	DatastreamIterator mockIter = mockDatastreamIterator(pid, dsid, "asdf");
    	when(mockDatastreams.getDatastreamsFor(pid)).thenReturn(mockIter);
    	ObjectDatastreams actual = testObj.getDatastreams(pid);
    	verify(mockDatastreams).getDatastreamsFor(pid);
    	verify(mockSession, never()).save();
    	assertEquals(1, actual.datastreams.size());
    	assertEquals(dsid, actual.datastreams.iterator().next().dsid);
    }
    
    @Test
    public void testModifyDatastreams() throws RepositoryException, IOException, InvalidChecksumException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId1 = "testDs1";
    	String dsId2 = "testDs2";
    	HashMap<String, String> atts = new HashMap<String,String>(2);
    	atts.put(dsId1, "asdf");
    	atts.put(dsId2, "sdfg");
    	List<Attachment> attachStreams = getStringsAsAttachments(atts);
    	Response actual = testObj.modifyDatastreams(
    			pid,
    			Arrays.asList(new String[]{dsId1,dsId2}),
    			attachStreams);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
    	verify(mockDatastreams).createDatastreamNode(
    			any(Session.class), eq(getDatastreamJcrNodePath(pid, dsId1)), 
    			anyString(), any(InputStream.class));
    	verify(mockDatastreams).createDatastreamNode(
    			any(Session.class), eq(getDatastreamJcrNodePath(pid, dsId2)), 
    			anyString(), any(InputStream.class));
    	verify(mockSession).save();
    }

    @Test
    public void testDeleteDatastreams() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	List<String> dsidList = Arrays.asList(new String[]{"ds1", "ds2"});
    	Response actual  = testObj.deleteDatastreams(pid, dsidList);
    	assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
    	verify(mockDatastreams).purgeDatastream(mockSession, pid, "ds1");
    	verify(mockDatastreams).purgeDatastream(mockSession, pid, "ds2");
    	verify(mockSession).save();
    }

    @Test
    public void testGetDatastreamsContents() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	String dsContent = "asdf";
    	Datastream mockDs = mockDatastream(pid, dsId, dsContent);
    	when(mockDatastreams.getDatastream(pid, dsId)).thenReturn(mockDs);
    	MultipartBody actual = testObj.getDatastreamsContents(pid, Arrays.asList(new String[]{dsId}));
    	verify(mockDatastreams).getDatastream(pid, dsId);
    	verify(mockDs).getDsId();
    	verify(mockDs).getContent();
    	verify(mockSession, never()).save();
    	List<Attachment> actualAttachments = actual.getAllAttachments();
    	assertEquals(1, actualAttachments.size());
    	InputStream actualContent = (InputStream) actual.getAttachment(dsId).getObject();
        assertEquals("asdf", IOUtils.toString(actualContent, "UTF-8"));
    }

    @Test
    public void testAddDatastream() throws RepositoryException, IOException, InvalidChecksumException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	String dsContent = "asdf";
    	String dsPath = getDatastreamJcrNodePath(pid, dsId);
    	InputStream dsContentStream = IOUtils.toInputStream(dsContent);
    	Response actual = testObj.addDatastream(pid, dsId, null, dsContentStream);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
    	verify(mockDatastreams).exists(pid, dsId, mockSession);
    	verify(mockDatastreams).createDatastreamNode(
    			any(Session.class), eq(dsPath), 
    			anyString(), any(InputStream.class),
    			anyString(), anyString());
    	verify(mockSession).save();
    }

    @Test
    public void testModifyDatastream() throws RepositoryException, IOException, InvalidChecksumException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	String dsContent = "asdf";
    	String dsPath = getDatastreamJcrNodePath(pid, dsId);
    	InputStream dsContentStream = IOUtils.toInputStream(dsContent);
    	Response actual = testObj.modifyDatastream(pid, dsId, null, dsContentStream);
    	assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
    	verify(mockDatastreams).createDatastreamNode(
    			any(Session.class), eq(dsPath), 
    			anyString(), any(InputStream.class));
    	verify(mockSession).save();
    }

    @Test
    public void testGetDatastream() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	Datastream mockDs = mockDatastream(pid, dsId, null);
    	when(mockDatastreams.getDatastream(pid, dsId)).thenReturn(mockDs);
    	DatastreamProfile actual = testObj.getDatastream(pid, dsId);
    	assertNotNull(actual);
    	verify(mockDatastreams).getDatastream(pid, dsId);
    	verify(mockSession, never()).save();
    }

    @Test
    public void testGetDatastreamContent() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	String dsContent = "asdf";
    	Datastream mockDs = mockDatastream(pid, dsId, dsContent);
    	when(mockDatastreams.getDatastream(pid, dsId)).thenReturn(mockDs);
    	Request mockRequest = mock(Request.class);
    	Response actual = testObj.getDatastreamContent(pid, dsId, mockRequest);
    	verify(mockDatastreams).getDatastream(pid, dsId);
    	verify(mockDs).getContent();
    	verify(mockSession, never()).save();
    	String actualContent = IOUtils.toString((InputStream) actual.getEntity());
        assertEquals("asdf", actualContent);
    }

    @Test
    public void testGetDatastreamHistory() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	Datastream mockDs = mockDatastream(pid, dsId, null);
    	when(mockDatastreams.getDatastream(pid, dsId)).thenReturn(mockDs);
    	DatastreamHistory actual = testObj.getDatastreamHistory(pid, dsId);
    	assertNotNull(actual);
    	verify(mockDatastreams).getDatastream(pid, dsId);
    	verify(mockSession, never()).save();
    }

    @Test
    public void testGetDatastreamFixity() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	Datastream mockDs = mockDatastream(pid, dsId, null);
    	when(mockDatastreams.getDatastream(pid, dsId)).thenReturn(mockDs);
    	DatastreamFixity actual = testObj.getDatastreamFixity(pid, dsId);
    	assertNotNull(actual);
    	verify(mockDatastreams).getDatastream(pid, dsId);
    	verify(mockLow).runFixityAndFixProblems(mockDs);
    	verify(mockSession, never()).save();
    }

    @Test
    public void testDeleteDatastream() throws RepositoryException, IOException {
    	String pid = "FedoraDatastreamsTest1";
    	String dsId = "testDS";
    	Response actual = testObj.deleteDatastream(pid, dsId);
    	assertNotNull(actual);
    	assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
    	verify(mockDatastreams).purgeDatastream(mockSession, pid, dsId);
    	verify(mockSession).save();
    }
}