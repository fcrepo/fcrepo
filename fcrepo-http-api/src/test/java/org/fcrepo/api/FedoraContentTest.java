/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;

public class FedoraContentTest {

    FedoraContent testObj;

    DatastreamService mockDatastreams;

    NodeService mockNodeService;

    Session mockSession;

    @Before
    public void setUp() throws Exception {
        mockDatastreams = mock(DatastreamService.class);
        mockNodeService = mock(NodeService.class);

        testObj = new FedoraContent();
        TestHelpers.setField(testObj, "datastreamService", mockDatastreams);
        TestHelpers.setField(testObj, "nodeService", mockNodeService);
        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        mockSession = TestHelpers.mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @Test
    public void testPutContent() throws RepositoryException, IOException,
            InvalidChecksumException, URISyntaxException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        final Node mockNode = mock(Node.class);
        when(mockNode.isNew()).thenReturn(true);
        final Node mockContentNode = mock(Node.class);
        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(
                mockContentNode);
        when(mockContentNode.getPath()).thenReturn(dsPath + "/jcr:content");
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
    public void testCreateContent() throws RepositoryException, IOException,
            InvalidChecksumException, URISyntaxException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "xyz";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        final Node mockNode = mock(Node.class);

        when(mockNode.isNew()).thenReturn(true);
        final Node mockContentNode = mock(Node.class);
        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(
                mockContentNode);
        when(mockContentNode.getPath()).thenReturn(dsPath + "/jcr:content");
        when(mockNodeService.exists(mockSession, dsPath)).thenReturn(false);
        when(
                mockDatastreams.createDatastreamNode(any(Session.class),
                        eq(dsPath), anyString(), any(InputStream.class),
                        eq((URI) null))).thenReturn(mockNode);
        when(mockDatastreams.exists(mockSession, dsPath)).thenReturn(true);
        final Response actual =
                testObj.create(createPathList(pid, dsId), null,
                        MediaType.TEXT_PLAIN_TYPE, dsContentStream);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(mockSession, dsPath,
                "text/plain", dsContentStream, null);
        verify(mockSession).save();
    }

    @Test
    public void testCreateContentAtMintedPath() throws RepositoryException,
            IOException, InvalidChecksumException, URISyntaxException,
            NoSuchFieldException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "fcr:new";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        final Node mockNode = mock(Node.class);

        final PidMinter mockMinter = mock(PidMinter.class);
        when(mockMinter.mintPid()).thenReturn("xyz");
        TestHelpers.setField(testObj, "pidMinter", mockMinter);
        when(mockNode.isNew()).thenReturn(true);
        final Node mockContentNode = mock(Node.class);
        when(mockNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(
                mockContentNode);
        when(mockContentNode.getPath()).thenReturn(dsPath + "/jcr:content");
        when(mockNodeService.exists(mockSession, dsPath)).thenReturn(false);
        when(
                mockDatastreams.createDatastreamNode(any(Session.class),
                        eq("/" + pid + "/xyz"), anyString(),
                        any(InputStream.class), eq((URI) null))).thenReturn(
                mockNode);
        when(mockDatastreams.exists(mockSession, dsPath)).thenReturn(true);
        final Response actual =
                testObj.create(createPathList(pid, dsId), null,
                        MediaType.TEXT_PLAIN_TYPE, dsContentStream);
        assertEquals(Status.CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastreamNode(mockSession,
                "/" + pid + "/xyz", "text/plain", dsContentStream, null);
        verify(mockSession).save();
    }

    @Test
    public void testModifyContent() throws RepositoryException, IOException,
            InvalidChecksumException, URISyntaxException {
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