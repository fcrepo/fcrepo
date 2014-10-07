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
package org.fcrepo.http.commons.api.rdf;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.ws.rs.core.UriBuilder;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FROZEN_NODE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

/**
 * @author cabeer
 */
public class UriAwareIdentifierConverterTest {

    @Mock
    private Session session;

    @Mock
    private Node node;

    @Mock
    private Node versionedNode;

    @Mock
    private Node contentNode;

    @Mock
    private Property mockProperty;


    private UriAwareIdentifierConverter converter;
    private String uriTemplate = "http://localhost:8080/some/{path: .*}";
    private String path = "arbitrary/path";
    private Resource resource = createResource("http://localhost:8080/some/" + path);
    private Resource versionedResource = createResource("http://localhost:8080/some/" + path + "/fcr:versions/x");
    private Resource metadataResource = createResource(resource.toString() + "/fcr:metadata");

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private VersionManager mockVersionManager;

    @Mock
    private VersionHistory mockVersionHistory;

    @Mock
    private Version mockVersion;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        final UriBuilder uriBuilder = UriBuilder.fromUri(uriTemplate);
        converter = new UriAwareIdentifierConverter(session, uriBuilder);
        when(session.getNode("/" + path)).thenReturn(node);
        when(node.getPath()).thenReturn("/" + path);
        when(node.isNodeType(FROZEN_NODE)).thenReturn(false);
        when(node.isNodeType(FEDORA_DATASTREAM)).thenReturn(false);
        when(contentNode.getPath()).thenReturn("/" + path + "/jcr:content");
        when(session.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(versionedNode.isNodeType("nt:frozenNode")).thenReturn(true);
    }

    @Test
    public void testDoForward() throws Exception {
        final Node converted = converter.convert(resource);
        assertEquals(node, converted);
    }

    @Test
    public void testDoForwardWithDatastreamContent() throws Exception {
        when(node.isNodeType(FEDORA_DATASTREAM)).thenReturn(true);
        when(node.getNode(JCR_CONTENT)).thenReturn(contentNode);
        final Node converted = converter.convert(resource);
        assertEquals(contentNode, converted);
    }

    @Test
    public void testDoForwardWithDatastreamMetadata() throws Exception {
        when(node.isNodeType(FEDORA_DATASTREAM)).thenReturn(true);
        final Node converted = converter.convert(metadataResource);
        assertEquals(node, converted);
    }

    @Test
    public void testDoBackward() throws Exception {
        final Resource converted = converter.reverse().convert(node);
        assertEquals(resource, converted);
    }

    @Test
    public void testDoBackwardWithDatastreamContent() throws Exception {
        final Resource converted = converter.reverse().convert(contentNode);
        assertEquals(resource, converted);
    }

    @Test
    public void testDoBackwardWithDatastreamMetadata() throws Exception {
        when(node.isNodeType(FEDORA_DATASTREAM)).thenReturn(true);
        final Resource converted = converter.reverse().convert(node);
        assertEquals(metadataResource, converted);
    }

    @Test
    public void testDoForwardWithImplicitVersionedDatastream() throws Exception {
        when(session.getNodeByIdentifier("x")).thenReturn(versionedNode);
        when(versionedNode.getProperty("jcr:frozenUuid")).thenReturn(mockProperty);
        when(mockProperty.getString()).thenReturn("some-identifier");
        when(node.getIdentifier()).thenReturn("some-identifier");
        final Node converted = converter.convert(versionedResource);
        assertEquals(versionedNode, converted);
    }

    @Test
    public void testDoForwardWithExplicitVersionedDatastream() throws Exception {
        when(session.getNodeByIdentifier("x")).thenThrow(new ItemNotFoundException());
        when(mockVersionManager.getVersionHistory("/" + path)).thenReturn(mockVersionHistory);
        when(mockVersionHistory.hasVersionLabel("x")).thenReturn(true);
        when(mockVersionHistory.getVersionByLabel("x")).thenReturn(mockVersion);
        when(mockVersion.getFrozenNode()).thenReturn(versionedNode);
        final Node converted = converter.convert(versionedResource);
        assertEquals(versionedNode, converted);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testDoForwardWithMissingVersionedDatastream() throws Exception {
        when(session.getNodeByIdentifier("x")).thenThrow(new ItemNotFoundException());
        when(mockVersionManager.getVersionHistory("/" + path)).thenReturn(mockVersionHistory);
        when(mockVersionHistory.hasVersionLabel("x")).thenReturn(false);
        converter.convert(versionedResource);
    }

    @Test
    public void testDoBackwardWithVersionedNode() throws Exception {

        when(versionedNode.getProperty("jcr:frozenUuid")).thenReturn(mockProperty);
        when(versionedNode.getIdentifier()).thenReturn("x");
        when(mockProperty.getString()).thenReturn("some-identifier");
        when(node.getIdentifier()).thenReturn("some-identifier");
        when(session.getNodeByIdentifier("some-identifier")).thenReturn(node);
        when(node.isNodeType("mix:versionable")).thenReturn(true);

        final Resource converted = converter.reverse().convert(versionedNode);
        assertEquals(versionedResource, converted);
    }
}