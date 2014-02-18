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

package org.fcrepo.http.api.versioning;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.api.rdf.GraphSubjectsTest;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VersionAwareHttpGraphSubjectsTest extends GraphSubjectsTest {

    @Mock
    protected VersionManager mockVersionManager;

    @Before
    public void setUpForThisClass() throws RepositoryException {
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
    }

    protected HttpGraphSubjects getTestObj() {
        return new VersionAwareHttpGraphSubjects(mockSession, mockSession,
                MockNodeController.class,
                uriInfo);
    }

    @Test
    public void testGetGraphSubjectForVersion() throws RepositoryException {
        mockVersion(UUID.randomUUID().toString(), null, true);
        Resource actual = testObj.getGraphSubject(mockNode);
        assertEquals(mockSubject.getURI(), actual.getURI());
    }

    @Test
    public void testGetNodeFromGraphSubjectForVersionByUUID() throws PathNotFoundException,
            RepositoryException {
        mockVersion(UUID.randomUUID().toString(), null, true);
        Node actual = testObj.getNodeFromGraphSubject(mockSubject);
        assertEquals(mockNode, actual);

        mockVersion(UUID.randomUUID().toString(), null, false);
        actual = testObj.getNodeFromGraphSubject(mockSubject);
        assertEquals(null, actual);
    }

    @Test
    public void testGetNodeFromGraphSubjectForVersionByLabel() throws PathNotFoundException,
            RepositoryException {
        mockVersion("test", "label", true);
        Node actual = testObj.getNodeFromGraphSubject(mockSubject);
        assertEquals(mockNode, actual);

        mockVersion("invalid", "missing-label", false);
        actual = testObj.getNodeFromGraphSubject(mockSubject);
        assertEquals(null, actual);
    }

    /**
     * Sets the mocks to represent an environment where the node at the given
     * path has a version with the given label (or not if null).
     */
    private void mockVersion(String nodePath, String label, boolean valid) throws RepositoryException {
        String frozenNodeUUID = UUID.randomUUID().toString();
        String versionedNodeUUID = UUID.randomUUID().toString();

        if (label != null) {
            when(mockSubject.getURI()).thenReturn("http://localhost:8080/fcrepo/rest/" + nodePath + "/fcr:versions/" + label);
            when(mockSubject.isURIResource()).thenReturn(true);

            when(mockSession.getNodeByIdentifier(label)).thenThrow(ItemNotFoundException.class);
            Version mockVersion = mock(Version.class);
            when(mockVersion.getFrozenNode()).thenReturn(mockNode);
            when(mockNode.getPath()).thenReturn("/" + nodePath);
            when(mockSession.nodeExists("/" + nodePath)).thenReturn(true);
            when(mockSession.getNode("/" + nodePath)).thenReturn(mockNode);
            VersionHistory mockVersionHistory = mock(VersionHistory.class);
            when(mockVersionHistory.hasVersionLabel(label)).thenReturn(valid);
            when(mockVersionHistory.getVersionByLabel(label)).thenReturn(mockVersion);
            when(mockVersionManager.getVersionHistory("/" + nodePath)).thenReturn(mockVersionHistory);
        } else {
            if (valid) {
                when(mockSession.getNodeByIdentifier(frozenNodeUUID)).thenReturn(mockNode);
            } else {
                when(mockSession.getNodeByIdentifier(frozenNodeUUID)).thenThrow(ItemNotFoundException.class);
            }
            when(mockSubject.getURI()).thenReturn(
                    "http://localhost:8080/fcrepo/rest/" + nodePath + "/fcr:versions/" + frozenNodeUUID);
            when(mockSubject.isURIResource()).thenReturn(true);

            // set up the version manager
            when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
            VersionHistory mockVersionHistory = mock(VersionHistory.class);
            when(mockVersionHistory.hasVersionLabel(any(String.class))).thenReturn(false);
            when(mockVersionManager.getVersionHistory("/" + nodePath)).thenReturn(mockVersionHistory);

            // set up frozen node
            when(mockNodeType.getName()).thenReturn("nt:frozenNode");
            when(mockNode.getPrimaryNodeType()).thenReturn(mockNodeType);
            when(mockNode.getPath()).thenReturn("/" + nodePath);
            Property mockProperty = mock(Property.class);
            when(mockProperty.getString()).thenReturn(versionedNodeUUID);
            when(mockNode.getProperty("jcr:frozenUuid")).thenReturn(mockProperty);
            when(mockNode.getIdentifier()).thenReturn(frozenNodeUUID);

            // set up the versioned node
            Node versionableNode = mock(Node.class);
            when(versionableNode.isNodeType("mix:versionable")).thenReturn(true);
            when(versionableNode.getPath()).thenReturn("/" + nodePath);
            when(mockSession.nodeExists("/" + nodePath)).thenReturn(true);
            when(mockSession.getNode("/" + nodePath)).thenReturn(mockNode);
            when(mockSession.getNodeByIdentifier(versionedNodeUUID)).thenReturn(versionableNode);
        }
    }
}
