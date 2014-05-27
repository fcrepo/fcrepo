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
package org.fcrepo.http.api.versioning;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.api.rdf.GraphSubjectsTest;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import java.util.UUID;

import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

/**
 * @author Mike Durbin
 * @since 2014-02-21
 */
public class VersionAwareHttpGraphSubjectsTest extends GraphSubjectsTest {

    @Mock
    protected VersionManager mockVersionManager;

    @Mock
    protected Node mockFrozenNode;

    @Mock
    protected Node mockVersionableNode;

    @Before
    public void setUpForThisClass() throws RepositoryException {
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockWorkspace.getName()).thenReturn("default");
    }

    @Override
    protected HttpIdentifierTranslator getTestObj() {
        return new VersionAwareHttpIdentifierTranslator(mockSession, mockSession,
                MockNodeController.class,
                uriInfo);
    }

    @Test
    public void testGetGraphSubject() throws RepositoryException {
        mockVersion(UUID.randomUUID().toString());

        final String uri = "http://localhost:8080/fcrepo/rest" + mockVersionableNode.getPath()
                + "/fcr:versions/" + mockFrozenNode.getIdentifier();

        final Resource actual = testObj.getSubject(mockFrozenNode.getPath());
        assertEquals(uri, actual.getURI());
    }

    @Test
    public void testGetGraphSubjectChildNode() throws RepositoryException {
        mockVersion(UUID.randomUUID().toString());

        final String uri = "http://localhost:8080/fcrepo/rest" + mockVersionableNode.getPath()
                + "/fcr:versions/" + mockFrozenNode.getIdentifier() + "/fcr:content";

        final String contentNodeId = UUID.randomUUID().toString();
        final Node mockVersionChildNode = mock(Node.class);
        when(mockSession.getNodeByIdentifier(contentNodeId)).thenReturn(mockVersionChildNode);
        when(mockVersionChildNode.isNodeType("mix:versionable")).thenReturn(false);
        final String mockVersionedChildNodePath = mockVersionableNode.getPath() + "/" + FCR_CONTENT;
        when(mockVersionChildNode.getPath()).thenReturn(mockVersionedChildNodePath);
        when(mockVersionChildNode.getParent()).thenReturn(mockVersionableNode);

        final Node mockFrozenChildNode = mock(Node.class);
        final String mockFrozenChildNodePath = mockFrozenNode.getPath() + "/" + JCR_CONTENT;
        when(mockFrozenChildNode.getPath()).thenReturn(mockFrozenChildNodePath);
        final NodeType mockFrozenNodeType = mock(NodeType.class);
        when(mockFrozenNodeType.getName()).thenReturn("nt:frozenNode");
        when(mockFrozenChildNode.getPrimaryNodeType()).thenReturn(mockFrozenNodeType);
        when(mockSession.nodeExists(mockFrozenChildNodePath)).thenReturn(true);
        when(mockSession.getNode(mockFrozenChildNodePath)).thenReturn(mockFrozenChildNode);
        final Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn(contentNodeId);
        when(mockFrozenChildNode.getProperty("jcr:frozenUuid")).thenReturn(mockProperty);
        when(mockFrozenChildNode.getPath()).thenReturn(mockFrozenChildNodePath);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockFrozenChildNode.getParent()).thenReturn(mockFrozenNode);

        final Resource actual = testObj.getSubject(mockFrozenChildNodePath);
        assertEquals(uri, actual.getURI());
    }


    @Test
    public void testGetGraphSubjectForVersion() throws RepositoryException {
        mockVersion(UUID.randomUUID().toString());

        final Resource actual = testObj.getSubject(mockFrozenNode.getPath());
        assertEquals("http://localhost:8080/fcrepo/rest" + mockVersionableNode.getPath() + "/fcr:versions/"
                + mockFrozenNode.getIdentifier(), actual.getURI());
    }

    @Test
    public void testGetNodeFromGraphSubjectForVersionByUUID() throws PathNotFoundException,
            RepositoryException {

        mockVersion(UUID.randomUUID().toString());

        final String uri = "http://localhost:8080/fcrepo/rest" + mockVersionableNode.getPath()
                + "/fcr:versions/" + mockFrozenNode.getIdentifier();
        mockSubject(uri);

        final String actual = testObj.getPathFromSubject(mockSubject);
        assertEquals(mockFrozenNode.getPath(), actual);

    }

    @Test
    public void testGetNodeFromGraphSubjectForVersionByLabel() throws PathNotFoundException,
            RepositoryException {
        mockVersion(UUID.randomUUID().toString());

        final String label = UUID.randomUUID().toString();

        when(mockSession.getNodeByIdentifier(label)).thenThrow(new ItemNotFoundException());
        final Version mockVersion = mock(Version.class);
        when(mockVersion.getFrozenNode()).thenReturn(mockFrozenNode);
        final VersionHistory mockVersionHistory = mock(VersionHistory.class);
        when(mockVersionHistory.hasVersionLabel(label)).thenReturn(true);
        when(mockVersionHistory.getVersionByLabel(label)).thenReturn(mockVersion);
        when(mockVersionManager.getVersionHistory(mockVersionableNode.getPath())).thenReturn(mockVersionHistory);

        final String uri = "http://localhost:8080/fcrepo/rest" + mockVersionableNode.getPath()
                + "/fcr:versions/" + label;
        mockSubject(uri);

        final String actual = testObj.getPathFromSubject(mockSubject);
        assertEquals(mockFrozenNode.getPath(), actual);
    }

    @Test
    public void testGetNodeFromGraphSubjectForVersionChildByUUID() throws PathNotFoundException,
            RepositoryException {

        mockVersion(UUID.randomUUID().toString());

        final String uri = "http://localhost:8080/fcrepo/rest" + mockVersionableNode.getPath()
                + "/fcr:versions/" + mockFrozenNode.getIdentifier() + "/fcr:content";
        mockSubject(uri);

        final Node mockVersionChildNode = mock(Node.class);
        final String mockVersionChildNodePath = mockFrozenNode.getPath() + "/" + JCR_CONTENT;
        when(mockSession.getNode(mockVersionChildNodePath)).thenReturn(mockVersionChildNode);
        when(mockVersionChildNode.getPath()).thenReturn(mockVersionChildNodePath);
        when(mockSession.nodeExists(mockVersionChildNodePath)).thenReturn(true);
        when(mockSession.getNode(mockVersionChildNodePath)).thenReturn(mockVersionChildNode);

        final String actual = testObj.getPathFromSubject(mockSubject);
        assertEquals(mockVersionChildNode.getPath(), actual);
    }

    private void mockSubject(final String uri) {
        when(mockSubject.getURI()).thenReturn(uri);
        when(mockSubject.isURIResource()).thenReturn(true);
    }

    /**
     * Sets up the mocks so that mockVersionableNode represents a new
     * versionable node and mockFrozenNode represents a historical version
     * of that node.
     */
    private void mockVersion(final String nodePath) throws RepositoryException {
        final String frozenNodeUUID = UUID.randomUUID().toString();
        final String versionedNodeUUID = UUID.randomUUID().toString();

        final String frozenPath = "/jcr:versionStorage/" + frozenNodeUUID;
        final String versionedPath = "/" + nodePath;

        when(mockSession.nodeExists(frozenPath)).thenReturn(true);
        when(mockSession.nodeExists(versionedPath)).thenReturn(true);
        when(mockSession.getNode(frozenPath)).thenReturn(mockFrozenNode);
        when(mockSession.getNodeByIdentifier(frozenNodeUUID)).thenReturn(mockFrozenNode);
        when(mockSession.getNode(versionedPath)).thenReturn(mockVersionableNode);
        when(mockSession.getNodeByIdentifier(versionedNodeUUID)).thenReturn(mockVersionableNode);
        when(mockVersionableNode.getIdentifier()).thenReturn(versionedNodeUUID);
        when(mockFrozenNode.getIdentifier()).thenReturn(frozenNodeUUID);
        when(mockVersionableNode.getPath()).thenReturn(versionedPath);

        final NodeType mockFrozenNodeType = mock(NodeType.class);
        when(mockFrozenNodeType.getName()).thenReturn("nt:frozenNode");
        when(mockFrozenNode.getPrimaryNodeType()).thenReturn(mockFrozenNodeType);
        when(mockFrozenNode.getPath()).thenReturn(frozenPath);
        final Property mockProperty = mock(Property.class);
        when(mockProperty.getString()).thenReturn(versionedNodeUUID);
        when(mockFrozenNode.getProperty("jcr:frozenUuid")).thenReturn(mockProperty);

        when(mockVersionableNode.isNodeType("mix:versionable")).thenReturn(true);

    }
}
