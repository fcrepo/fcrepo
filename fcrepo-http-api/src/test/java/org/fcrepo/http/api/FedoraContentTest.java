/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.http.api;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockDatastream;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.identifiers.PidMinter;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.VersionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraContentTest class.</p>
 *
 * @author awoods
 */
public class FedoraContentTest {

    FedoraContent testObj;

    @Mock
    private DatastreamService mockDatastreams;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private VersionService mockVersions;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private Node mockContentNode;

    @Mock
    private NodeType mockContentNodeType;

    @Mock
    private PidMinter mockMinter;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private Datastream mockDatastream;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraContent();
        setField(testObj, "datastreamService", mockDatastreams);
        setField(testObj, "nodeService", mockNodeService);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "versionService", mockVersions);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);
        when(mockNodeType.getName()).thenReturn("nt:file");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockContentNodeType.getName()).thenReturn("nt:content");
        when(mockContentNode.getPrimaryNodeType()).thenReturn(mockContentNodeType);
        when(mockDatastream.getContentDigest()).thenReturn(URI.create("some:uri"));
        when(mockDatastream.getContentNode()).thenReturn(mockContentNode);
    }

    @Test
    public void testPutContent()
            throws RepositoryException, InvalidChecksumException, URISyntaxException, ParseException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        when(mockDatastream.isNew()).thenReturn(true);
        when(mockContentNode.getPath()).thenReturn(dsPath + "/jcr:content");
        when(mockNodeService.exists(mockSession, dsPath)).thenReturn(false);
        when(
                mockDatastreams.createDatastream(any(Session.class),
                        eq(dsPath), anyString(), eq("xyz"), any(InputStream.class), eq((URI)null)))
                .thenReturn(mockDatastream);
        when(mockDatastreams.exists(mockSession, dsPath)).thenReturn(true);
        final Response actual =
            testObj.modifyContent(createPathList(pid, dsId), null, "inline; filename=\"xyz\"", null,
                    dsContentStream, null, mockResponse);
        assertEquals(CREATED.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastream(any(Session.class),
                eq(dsPath), anyString(), eq("xyz"), any(InputStream.class), eq((URI)null));
        verify(mockSession).save();
    }

    @Test
    public void testModifyContent()
            throws RepositoryException, InvalidChecksumException, URISyntaxException, ParseException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String dsContent = "asdf";
        final String dsPath = "/" + pid + "/" + dsId;
        final URI checksum = new URI("urn:sha1:some-checksum");
        final InputStream dsContentStream = IOUtils.toInputStream(dsContent);
        when(mockNode.isNew()).thenReturn(false);
        final Datastream mockDs = mockDatastream(pid, dsId, dsContent);
        when(mockDatastreams.asDatastream(mockNode)).thenReturn(mockDs);
        when(mockDatastreams.getDatastream(mockSession, dsPath)).thenReturn(
                mockDs);
        final Request mockRequest = mock(Request.class);
        when(
                mockRequest.evaluatePreconditions(any(Date.class),
                        any(EntityTag.class))).thenReturn(null);
        when(
                mockDatastreams.createDatastream(any(Session.class),
                                                 eq(dsPath),
                                                 anyString(),
                                                 eq((String) null),
                                                 any(InputStream.class),
                                                 eq(checksum)))
                .thenReturn(mockDatastream);
        when(mockDatastreams.exists(mockSession, dsPath)).thenReturn(true);
        final Response actual =
            testObj.modifyContent(createPathList(pid, dsId), "urn:sha1:some-checksum", null, null,
                    dsContentStream, mockRequest, mockResponse);
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockDatastreams).createDatastream(any(Session.class),
                                                 eq(dsPath),
                                                 anyString(),
                                                 eq((String) null),
                                                 any(InputStream.class),
                                                 eq(checksum));
        verify(mockSession).save();
    }

    @Test
    public void testGetContent() throws RepositoryException, IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String dsId = "testDS";
        final String path = "/" + pid + "/" + dsId;
        final String dsContent = "asdf";
        final Datastream mockDs = mockDatastream(pid, dsId, dsContent);
        when(mockDatastreams.getDatastream(mockSession, path)).thenReturn(
                mockDs);
        when(mockDs.getNode()).thenReturn(mockNode);
        when(mockNode.getPath()).thenReturn(path);
        final Request mockRequest = mock(Request.class);
        final Response actual =
            testObj.getContent(createPathList(pid, dsId), null, mockRequest, mockResponse);
        verify(mockDs).getContent();
        verify(mockSession, never()).save();
        final String actualContent =
            IOUtils.toString((InputStream) actual.getEntity());
        assertEquals("asdf", actualContent);
    }

}
