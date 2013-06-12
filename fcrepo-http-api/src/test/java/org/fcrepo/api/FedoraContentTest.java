
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
import java.util.Date;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FedoraContentTest {

    FedoraContent testObj;

    DatastreamService mockDatastreams;

    NodeService mockNodeService;

    Session mockSession;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        mockDatastreams = mock(DatastreamService.class);
        mockNodeService = mock(NodeService.class);

        testObj = new FedoraContent();
        testObj.setDatastreamService(mockDatastreams);
        testObj.setNodeService(mockNodeService);
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setSession(mockSession);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testPutContent() throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        final Node mockNode = mock(Node.class);
        when(mockNode.isNew()).thenReturn(true);
        when(mockNodeService.exists(mockSession, dsPath)).thenReturn(false);
        when(
                mockDatastreams.createDatastreamNode(any(Session.class),
                        eq(dsPath), anyString(), any(InputStream.class)))
                .thenReturn(mockNode);
        when(mockDatastreams.exists(mockSession, dsPath)).thenReturn(true);
        final Response actual =
                testObj.modifyContent(createPathList(pid, dsId), null,
                        dsContentStream, null);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq(dsPath), anyString(), any(InputStream.class));
        verify(mockSession).save();
    }

    @Test
    public void testModifyContent() throws RepositoryException, IOException,
            InvalidChecksumException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        final Node mockNode = mock(Node.class);
        when(mockNode.isNew()).thenReturn(false);

        final Datastream mockDs =
                TestHelpers.mockDatastream(pid, dsId, dsContent);
        when(mockDatastreams.getDatastream(mockSession, dsPath)).thenReturn(
                mockDs);
        final Request mockRequest = mock(Request.class);
        when(
                mockRequest.evaluatePreconditions(any(Date.class),
                        any(EntityTag.class))).thenReturn(null);
        when(
                mockDatastreams.createDatastreamNode(any(Session.class),
                        eq(dsPath), anyString(), any(InputStream.class)))
                .thenReturn(mockNode);
        when(mockDatastreams.exists(mockSession, dsPath)).thenReturn(true);
        final Response actual =
                testObj.modifyContent(createPathList(pid, dsId), null,
                        dsContentStream, mockRequest);
        assertEquals(Status.NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(any(Session.class),
                eq(dsPath), anyString(), any(InputStream.class));
        verify(mockSession).save();
    }

    @Test
    public void testGetContent() throws RepositoryException, IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String path = "/" + pid + "/" + dsId;
        final String dsContent = "asdf";
        final Datastream mockDs =
                TestHelpers.mockDatastream(pid, dsId, dsContent);
        when(mockDatastreams.getDatastream(mockSession, path)).thenReturn(
                mockDs);
        final Request mockRequest = mock(Request.class);
        final Response actual =
                testObj.getContent(createPathList(pid, dsId), mockRequest);
        verify(mockDs).getContent();
        verify(mockSession, never()).save();
        final String actualContent =
                IOUtils.toString((InputStream) actual.getEntity());
        assertEquals("asdf", actualContent);
    }

}