
package org.fcrepo.api;

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.NodeService;
import org.fcrepo.session.SessionFactory;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.Repository;

import com.sun.jersey.multipart.MultiPart;

public class FedoraDatastreamsTest {

    FedoraDatastreams testObj;

    DatastreamService mockDatastreams;

    NodeService mockNodes;

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
        mockNodes = mock(NodeService.class);

        testObj = new FedoraDatastreams();
        testObj.setNodeService(mockNodes);
        testObj.setDatastreamService(mockDatastreams);
        testObj.setSecurityContext(mockSecurityContext);
        testObj.setHttpServletRequest(mockServletRequest);
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
                testObj.modifyDatastreams(createPathList(pid), Arrays
                        .asList(new String[] {dsId1, dsId2}), multipart);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq("/" + pid + "/" + dsId1), anyString(),
                any(InputStream.class));
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq("/" + pid + "/" + dsId2), anyString(),
                any(InputStream.class));
        verify(mockSession).save();
    }

    @Test
    public void testDeleteDatastreams() throws RepositoryException, IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/" + pid;
        final List<String> dsidList =
                Arrays.asList(new String[] {"ds1", "ds2"});
        final Response actual =
                testObj.deleteDatastreams(createPathList(pid), dsidList);
        assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockNodes).deleteObject(mockSession, path + "/" + "ds1");
        verify(mockNodes).deleteObject(mockSession, path + "/" + "ds2");
        verify(mockSession).save();
    }

    @Test
    public void testGetDatastreamsContents() throws RepositoryException,
            IOException, NoSuchAlgorithmException {
        final Request mockRequest = mock(Request.class);
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";

        final Datastream mockDs =
                TestHelpers.mockDatastream(pid, dsId, dsContent);
        when(mockDs.hasContent()).thenReturn(true);
        final Node mockDsNode = mock(Node.class);
        final FedoraResource mockObject = mock(FedoraResource.class);
        final Node mockNode = mock(Node.class);
        final NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.nextNode()).thenReturn(mockDsNode);

        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getNodes(new String[] {dsId})).thenReturn(mockIterator);

        when(mockNodes.getObject(mockSession, "/FedoraDatastreamsTest1"))
                .thenReturn(mockObject);
        when(mockDatastreams.asDatastream(mockDsNode)).thenReturn(mockDs);

        final Response resp =
                testObj.getDatastreamsContents(createPathList(pid), Arrays
                        .asList(dsId), mockRequest);
        final MultiPart multipart = (MultiPart) resp.getEntity();

        verify(mockDs).getContent();
        verify(mockSession, never()).save();
        assertEquals(1, multipart.getBodyParts().size());
        final InputStream actualContent =
                (InputStream) multipart.getBodyParts().get(0).getEntity();
        assertEquals("/FedoraDatastreamsTest1/testDS", multipart.getBodyParts().get(0).getContentDisposition().getFileName());
        assertEquals("asdf", IOUtils.toString(actualContent, "UTF-8"));
    }

    @Test
    public void testGetDatastreamsContentsCached() throws RepositoryException,
            IOException, NoSuchAlgorithmException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final Datastream mockDs =
                TestHelpers.mockDatastream(pid, dsId, dsContent);
        final Node mockDsNode = mock(Node.class);
        final FedoraResource mockObject = mock(FedoraResource.class);
        final Node mockNode = mock(Node.class);
        final NodeIterator mockIterator = mock(NodeIterator.class);
        when(mockIterator.hasNext()).thenReturn(true, false);
        when(mockIterator.nextNode()).thenReturn(mockDsNode);

        when(mockObject.getNode()).thenReturn(mockNode);
        when(mockNode.getNodes(new String[] {dsId})).thenReturn(mockIterator);

        when(mockNodes.getObject(mockSession, "/FedoraDatastreamsTest1"))
                .thenReturn(mockObject);
        when(mockDatastreams.asDatastream(mockDsNode)).thenReturn(mockDs);

        final Request mockRequest = mock(Request.class);
        when(
                mockRequest.evaluatePreconditions(any(Date.class),
                        any(EntityTag.class))).thenReturn(
                Response.notModified());

        final Response resp =
                testObj.getDatastreamsContents(createPathList(pid), Arrays
                        .asList(dsId), mockRequest);
        verify(mockDs, never()).getContent();
        verify(mockSession, never()).save();
        assertEquals(Status.NOT_MODIFIED.getStatusCode(), resp.getStatus());
    }

}