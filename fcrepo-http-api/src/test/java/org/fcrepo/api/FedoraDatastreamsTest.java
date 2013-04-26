
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
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
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.jaxb.responses.access.ObjectDatastreams;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.jaxb.responses.management.DatastreamHistory;
import org.fcrepo.jaxb.responses.management.DatastreamProfile;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.utils.DatastreamIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

import com.sun.jersey.multipart.MultiPart;

public class FedoraDatastreamsTest {

    FedoraDatastreams testObj;

    DatastreamService mockDatastreams;

    LowLevelStorageService mockLow;

    Repository mockRepo;

    Session mockSession;

    SecurityContext mockSecurityContext;

    HttpServletRequest mockServletRequest;

    Principal mockPrincipal;

    String mockUser = "testuser";

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockSecurityContext = mock(SecurityContext.class);
        mockServletRequest = mock(HttpServletRequest.class);
        mockPrincipal = mock(Principal.class);
        //Function<HttpServletRequest, Session> mockFunction = mock(Function.class);
        mockDatastreams = mock(DatastreamService.class);
        mockLow = mock(LowLevelStorageService.class);

        testObj = new FedoraDatastreams();
        testObj.setDatastreamService(mockDatastreams);
        testObj.setSecurityContext(mockSecurityContext);
        testObj.setHttpServletRequest(mockServletRequest);
        testObj.setLlStoreService(mockLow);
        //mockRepo = mock(Repository.class);
        final SessionFactory mockSessions = mock(SessionFactory.class);
        testObj.setSessionFactory(mockSessions);
        mockSession = mock(Session.class);
        when(
                mockSessions.getSession(any(SecurityContext.class),
                        any(HttpServletRequest.class))).thenReturn(mockSession);
        when(mockSession.getUserID()).thenReturn(mockUser);
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockPrincipal.getName()).thenReturn(mockUser);

        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetDatastreams() throws RepositoryException, IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid;
        final String dsid = "testDS";
        final DatastreamIterator mockIter =
                org.fcrepo.TestHelpers.mockDatastreamIterator(pid, dsid, "asdf");
        when(mockDatastreams.getDatastreamsForPath(path)).thenReturn(mockIter);
        final ObjectDatastreams actual = testObj.getDatastreams(createPathList("objects",pid));
        verify(mockDatastreams).getDatastreamsForPath(path);
        verify(mockSession, never()).save();
        assertEquals(1, actual.datastreams.size());
        assertEquals(dsid, actual.datastreams.iterator().next().dsid);
    }

    @Test
    public void testModifyDatastreams() throws RepositoryException,
            IOException, InvalidChecksumException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId1 = "testDs1";
        final String dsId2 = "testDs2";
        final HashMap<String, String> atts = new HashMap<String, String>(2);
        atts.put(dsId1, "asdf");
        atts.put(dsId2, "sdfg");
        final MultiPart multipart = TestHelpers.getStringsAsMultipart(atts);
        final Response actual =
                testObj.modifyDatastreams(createPathList("objects",pid), Arrays.asList(new String[] {
                        dsId1, dsId2}), multipart);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq(getDatastreamJcrNodePath(pid, dsId1)), anyString(),
                any(InputStream.class));
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq(getDatastreamJcrNodePath(pid, dsId2)), anyString(),
                any(InputStream.class));
        verify(mockSession).save();
    }

    @Test
    public void testDeleteDatastreams() throws RepositoryException, IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid;
        final List<String> dsidList =
                Arrays.asList(new String[] {"ds1", "ds2"});
        final Response actual = testObj.deleteDatastreams(createPathList("objects",pid), dsidList);
        assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).purgeDatastream(mockSession, path, "ds1");
        verify(mockDatastreams).purgeDatastream(mockSession, path, "ds2");
        verify(mockSession).save();
    }

    @Test
    public void testGetDatastreamsContents() throws RepositoryException,
            IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid;
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final Datastream mockDs = org.fcrepo.TestHelpers.mockDatastream(pid, dsId, dsContent);
        when(mockDatastreams.getDatastream(path, dsId)).thenReturn(mockDs);

        final Response resp =
                testObj.getDatastreamsContents(createPathList("objects",pid), Arrays
                        .asList(new String[] {dsId}));
        final MultiPart multipart = (MultiPart) resp.getEntity();

        verify(mockDs).getContent();
        verify(mockSession, never()).save();
        assertEquals(1, multipart.getBodyParts().size());
        final InputStream actualContent =
                (InputStream) multipart.getBodyParts().get(0).getEntity();
        assertEquals("asdf", IOUtils.toString(actualContent, "UTF-8"));
    }

    @Test
    public void testGetDatastreamHistory() throws RepositoryException,
            IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid;
        final String dsId = "testDS";
        final Datastream mockDs = org.fcrepo.TestHelpers.mockDatastream(pid, dsId, null);
        when(mockDatastreams.getDatastream(path, dsId)).thenReturn(mockDs);
        final DatastreamHistory actual =
                testObj.getDatastreamHistory(createPathList("objects",pid), dsId);
        assertNotNull(actual);
        verify(mockDatastreams).getDatastream(path, dsId);
        verify(mockSession, never()).save();
    }

    @Test
    public void testGetDatastreamFixity() throws RepositoryException,
            IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid;
        final String dsId = "testDS";
        final Datastream mockDs = org.fcrepo.TestHelpers.mockDatastream(pid, dsId, null);
        when(mockDatastreams.getDatastream(path, dsId)).thenReturn(mockDs);
        final DatastreamFixity actual = testObj.getDatastreamFixity(createPathList("objects",pid), dsId);
        assertNotNull(actual);
        verify(mockLow).runFixityAndFixProblems(mockDs);
        verify(mockSession, never()).save();
    }

}