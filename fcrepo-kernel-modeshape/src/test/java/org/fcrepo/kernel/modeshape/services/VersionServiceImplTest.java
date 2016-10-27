/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.services;

import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.VERSIONABLE;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Mike Durbin
 */
@RunWith(MockitoJUnitRunner.class)
public class VersionServiceImplTest {

    public static final String EXAMPLE_VERSIONED_PATH = "/example-versioned";
    public static final String EXAMPLE_UNVERSIONED_PATH = "/example-unversioned";

    private VersionService testObj;

    private FedoraSession testSession;

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private VersionManager mockVM;

    @Mock
    private Node unversionedNode;

    @Before
    public void setup() throws Exception {
        testObj = new VersionServiceImpl();
        testSession = new FedoraSessionImpl(mockSession);

        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getSession()).thenReturn(mockSession);
        when(mockWorkspace.getName()).thenReturn("default");
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);
        when(unversionedNode.getPath()).thenReturn(EXAMPLE_UNVERSIONED_PATH);
        when(unversionedNode.getSession()).thenReturn(mockSession);
        when(unversionedNode.isNodeType(VERSIONABLE))
                .thenReturn(false);
        when(mockSession.getNode(EXAMPLE_UNVERSIONED_PATH)).thenReturn(unversionedNode);
    }


    @Test
    public void testRevertToVersionByLabel() throws RepositoryException {
        final String versionLabel = "v";
        final VersionManager mockVersionManager = mock(VersionManager.class);
        final VersionHistory mockHistory = mock(VersionHistory.class);
        final Version mockVersion1 = mock(Version.class);
        final Version mockPreRevertVersion = mock(Version.class);
        when(mockVersionManager.checkin(EXAMPLE_VERSIONED_PATH)).thenReturn(mockPreRevertVersion);
        when(mockPreRevertVersion.getContainingHistory()).thenReturn(mockHistory);
        when(mockHistory.hasVersionLabel(versionLabel)).thenReturn(true);
        when(mockHistory.getVersionByLabel(versionLabel)).thenReturn(mockVersion1);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory(EXAMPLE_VERSIONED_PATH)).thenReturn(mockHistory);

        testObj.revertToVersion(testSession, EXAMPLE_VERSIONED_PATH, versionLabel);
        verify(mockVersionManager).restore(mockVersion1, true);

        verify(mockVersionManager, never()).checkpoint(EXAMPLE_VERSIONED_PATH);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testRevertToUnknownVersion() throws RepositoryException {
        final String versionUUID = "uuid";
        final VersionManager mockVersionManager = mock(VersionManager.class);
        final VersionHistory mockHistory = mock(VersionHistory.class);

        when(mockHistory.getVersionByLabel(versionUUID)).thenThrow(new VersionException());
        final VersionIterator mockVersionIterator = mock(VersionIterator.class);
        when(mockHistory.getAllVersions()).thenReturn(mockVersionIterator);
        when(mockVersionIterator.hasNext()).thenReturn(false);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory(EXAMPLE_VERSIONED_PATH)).thenReturn(mockHistory);

        testObj.revertToVersion(testSession, EXAMPLE_VERSIONED_PATH, versionUUID);
    }

    @Test
    public void testRemoveVersionByLabel() throws RepositoryException {
        final String versionLabel = "versionName";
        final String versionUUID = "uuid";
        final String versionName = "Bob";
        final String[] versionLabels = new String[]{ versionLabel };
        final VersionManager mockVersionManager = mock(VersionManager.class);
        final VersionHistory mockHistory = mock(VersionHistory.class);
        final Version mockVersion1 = mock(Version.class);
        final Version mockVersion2 = mock(Version.class);
        when(mockVersion1.getContainingHistory()).thenReturn(mockHistory);
        when(mockHistory.hasVersionLabel(versionLabel)).thenReturn(true);
        when(mockHistory.getVersionByLabel(versionLabel)).thenReturn(mockVersion1);
        when(mockHistory.getVersionLabels(mockVersion1)).thenReturn(versionLabels);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example")).thenReturn(mockHistory);
        when(mockVersionManager.getBaseVersion("/example")).thenReturn(mockVersion2);
        final Node mockFrozenNode = mock(Node.class);
        when(mockVersion1.getFrozenNode()).thenReturn(mockFrozenNode);
        when(mockFrozenNode.getIdentifier()).thenReturn(versionUUID);
        when(mockVersion1.getIdentifier()).thenReturn(versionUUID);
        when(mockVersion1.getName()).thenReturn(versionName);

        testObj.removeVersion(testSession, "/example", versionLabel);
        verify(mockHistory).removeVersion(versionName);
        verify(mockHistory).removeVersionLabel(versionLabel);
        verify(mockVersionManager, never()).checkpoint("/example");
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testRemoveUnknownVersion() throws RepositoryException {
        final String versionUUID = "uuid";
        final VersionManager mockVersionManager = mock(VersionManager.class);
        final VersionHistory mockHistory = mock(VersionHistory.class);

        when(mockHistory.getVersionByLabel(versionUUID)).thenThrow(new VersionException());
        final VersionIterator mockVersionIterator = mock(VersionIterator.class);
        when(mockHistory.getAllVersions()).thenReturn(mockVersionIterator);
        when(mockVersionIterator.hasNext()).thenReturn(false);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example")).thenReturn(mockHistory);

        testObj.removeVersion(testSession, "/example", versionUUID);
    }

    @Test
    public void testMixinCreationWhenExplicitlyVersioning() throws RepositoryException {
        testCreateWithValidLabel("LABEL");
    }

    @Test
    public void testCreateWithLabelv001() throws RepositoryException {
        testCreateWithValidLabel("v0.0.1");
    }

    @Test
    public void testCreateWithLabelWithSpace() throws RepositoryException {
        testCreateWithValidLabel("version v1234");
    }

    /**
     * This test designed to be pass so long as no exception is thrown.
     * @param label
     * @throws RepositoryException
     */
    private void testCreateWithValidLabel(final String label) throws RepositoryException {
        final VersionManager mockVersionManager = mock(VersionManager.class);
        final VersionHistory mockHistory = mock(VersionHistory.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory(EXAMPLE_UNVERSIONED_PATH)).thenReturn(mockHistory);
        testObj.createVersion(testSession, EXAMPLE_UNVERSIONED_PATH, label);

        final Node unversionedNode = mockSession.getNode(EXAMPLE_UNVERSIONED_PATH);
        verify(unversionedNode).isNodeType(VERSIONABLE);
        verify(unversionedNode).addMixin(VERSIONABLE);
    }

    @Test
    public void testCreateWithInvalidVersionCharacters() throws RepositoryException {
        final String[] invalidLabels = { "~", "#", "@", "*", "+", "%", "{", "}",
            "<", ">", "[", "]", "|", "\"", "^", "1234",
            "label ending in whitespace followed by a number 123","label ending with space "};

        for (String s : invalidLabels) {
            testInvalidLabel(s);
        }
    }

    private void testInvalidLabel(final String label) {
        try {
            testObj.createVersion(testSession, EXAMPLE_UNVERSIONED_PATH, label);
            fail("Expected failure on label \"" + label + "\" did not occur.");
        } catch (Exception ex) {
            final Throwable cause = ex.getCause();
            if (!(cause instanceof VersionException)) {
                fail("Expected VersionException on label \"" + label + "\": actual exception = " +
                        cause);
            }
        }
    }

}
