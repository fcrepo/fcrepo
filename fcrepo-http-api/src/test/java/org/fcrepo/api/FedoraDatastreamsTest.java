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
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

public class FedoraDatastreamsTest {

    FedoraDatastreams testObj;

    DatastreamService mockDatastreams;

    NodeService mockNodes;

    Session mockSession;

    @Before
    public void setUp() throws Exception {
        mockDatastreams = mock(DatastreamService.class);
        mockNodes = mock(NodeService.class);

        testObj = new FedoraDatastreams();
        TestHelpers.setField(testObj, "datastreamService", mockDatastreams);
        TestHelpers.setField(testObj, "nodeService", mockNodes);
        TestHelpers.setField(testObj, "uriInfo", TestHelpers.getUriInfoImpl());
        mockSession = TestHelpers.mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @Test
    public void testModifyDatastreams() throws RepositoryException,
            IOException, InvalidChecksumException, URISyntaxException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId1 = "testDs1";
        final String dsId2 = "testDs2";
        final HashMap<String, String> atts = new HashMap<String, String>(2);
        atts.put(dsId1, "asdf");
        atts.put(dsId2, "sdfg");
        final MultiPart multipart = getStringsAsMultipart(atts);
        final Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn("/FedoraDatastreamsTest1");
        when(mockSession.getNode("/FedoraDatastreamsTest1")).thenReturn(
                mockNode);
        final Response actual =
                testObj.modifyDatastreams(createPathList(pid), Arrays.asList(
                        dsId1, dsId2), multipart);
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
        final List<String> dsidList = Arrays.asList("ds1", "ds2");
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
        assertEquals("/FedoraDatastreamsTest1/testDS", multipart.getBodyParts()
                .get(0).getContentDisposition().getFileName());
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

    public static MultiPart getStringsAsMultipart(
            final Map<String, String> contents) {
        final MultiPart multipart = new MultiPart();
        for (final Map.Entry<String, String> e : contents.entrySet()) {
            final String id = e.getKey();
            final String content = e.getValue();
            final InputStream src = IOUtils.toInputStream(content);
            final StreamDataBodyPart part = new StreamDataBodyPart(id, src);
            try {
                final FormDataContentDisposition cd =
                        new FormDataContentDisposition("form-data;name=" + id +
                                ";filename=" + id + ".txt");
                part.contentDisposition(cd);
            } catch (final ParseException ex) {
                ex.printStackTrace();
            }
            multipart.bodyPart(part);
        }
        return multipart;
    }

}