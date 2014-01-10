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

package org.fcrepo.http.api;

import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.commons.test.util.TestHelpers;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;

public class FedoraVersionsTest {

    private FedoraVersions testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    VersionService mockVersions;

    @Mock
    private Node mockNode;

    private Session mockSession;

    @Mock
    private FedoraResourceImpl mockResource;

    private RdfStream mockRdfStream = new RdfStream();

    @Mock
    private Request mockRequest;

    @Mock
    private Variant mockVariant;

    @Mock
    private Dataset mockDataset;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraVersions();
        mockSession = mockSession(testObj);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", mockSession);
        setField(testObj, "versionService", mockVersions);
        when(mockNode.getPath()).thenReturn("/test/path");
        when(mockResource.getNode()).thenReturn(mockNode);
    }

    @Test
    public void testGetVersionList() throws RepositoryException {
        final String pid = "FedoraVersioningTest";
        when(mockRequest.selectVariant(POSSIBLE_RDF_VARIANTS)).thenReturn(
                mockVariant);
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);
        when(mockResource.getVersionTriples(any(HttpGraphSubjects.class)))
                .thenReturn(mockRdfStream);
        when(mockVariant.getMediaType()).thenReturn(
                new MediaType("text", "turtle"));
        final RdfStream response =
            testObj.getVersionList(createPathList(pid), mockRequest,
                    getUriInfoImpl());
        assertEquals("Got wrong RdfStream!", mockRdfStream, response);
    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {
        final String pid = "FedoraVersioningTest";
        final String versionLabel = "FedoraVersioningTest1/fcr:versions/v0.0.1";
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);

        final Response response =
            testObj.addVersion(createPathList(pid), versionLabel);
        verify(mockResource).addVersionLabel(anyString());
        verify(mockVersions).createVersion(any(Workspace.class),
                Matchers.<Collection<String>> any());
        assertNotNull(response);
    }

    @Test
    public void testGetVersion() throws RepositoryException, IOException {
        final String pid = "FedoraVersioningTest";
        final String versionLabel = "v0.0.1";
        when(
                mockNodes.getObject(any(Session.class), any(String.class),
                        any(String.class))).thenReturn(mockResource);
        when(mockRequest.selectVariant(POSSIBLE_RDF_VARIANTS)).thenReturn(
                mockVariant);
        when(mockVariant.getMediaType()).thenReturn(
                new MediaType("text", "turtle"));
        when(mockResource.getTriples(any(GraphSubjects.class))).thenReturn(mockRdfStream);
        final RdfStream response = testObj.getVersion(createPathList(pid), versionLabel, mockRequest, TestHelpers
                .getUriInfoImpl());
        assertEquals("Got wrong triples!", mockRdfStream, response);
    }

}
