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

import static javax.jcr.PropertyType.PATH;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collection;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraResourceImpl;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;

/**
 * <p>FedoraVersionsTest class.</p>
 *
 * @author awoods
 */
public class FedoraVersionsTest {

    private FedoraVersions testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    VersionService mockVersions;

    @Mock
    SessionFactory mockSessionFactory;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

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

    @Mock
    private Value mockValue;

    @Mock
    private ValueFactory mockValueFactory;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraVersions();
        mockSession = mockSession(testObj);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        setField(testObj, "session", mockSession);
        setField(testObj, "versionService", mockVersions);
        setField(testObj, "sessionFactory", mockSessionFactory);
        when(mockSessionFactory.getInternalSession()).thenReturn(mockSession);
        when(mockNode.getPath()).thenReturn("/test/path");
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNodeType.getName()).thenReturn("nt:folder");
        when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
    }

    @Test
    public void testGetVersionList() throws RepositoryException {
        final String pid = "FedoraVersioningTest";
        when(mockRequest.selectVariant(POSSIBLE_RDF_VARIANTS)).thenReturn(
                mockVariant);
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);
        when(mockResource.getVersionTriples(any(HttpIdentifierTranslator.class)))
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
                Matchers.<Collection<String>>any());
        assertNotNull(response);
    }

    @Test
    public void testRevertToVersion() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final String versionLabel = UUID.randomUUID().toString();
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);
        final Response response = testObj.revertToVersion(createPathList(pid), versionLabel);
        verify(mockVersions).revertToVersion(testObj.session.getWorkspace(), "/" + pid, versionLabel);
        assertNotNull(response);
    }

    @Test (expected = PathNotFoundException.class)
    public void testRevertToVersionFailure() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final String versionLabel = UUID.randomUUID().toString();
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);
        doThrow(PathNotFoundException.class)
                .when(mockVersions).revertToVersion(any(Workspace.class), anyString(), anyString());
        testObj.revertToVersion(createPathList(pid), versionLabel);
    }

    @Test
    public void testRemoveVersion() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final String versionLabel = UUID.randomUUID().toString();
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);
        final Response response = testObj.removeVersion(createPathList(pid), versionLabel);
        verify(mockVersions).removeVersion(testObj.session.getWorkspace(), "/" + pid, versionLabel);
        assertNotNull(response);
    }

    @Test (expected = PathNotFoundException.class)
    public void testRemoveVersionFailure() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final String versionLabel = UUID.randomUUID().toString();
        when(mockNodes.getObject(any(Session.class), anyString())).thenReturn(
                mockResource);
        doThrow(PathNotFoundException.class)
                .when(mockVersions).removeVersion(any(Workspace.class), anyString(), anyString());
        testObj.removeVersion(createPathList(pid), versionLabel);
    }

}
