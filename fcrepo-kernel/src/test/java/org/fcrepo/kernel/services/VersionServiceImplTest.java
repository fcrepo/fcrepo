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

package org.fcrepo.kernel.services;

import org.fcrepo.kernel.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Mike Durbin
 */
public class VersionServiceImplTest {

    private static final String USER_NAME = "test";

    private VersionService testObj;

    private TransactionService txService;

    @Mock
    private Session s;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private VersionManager mockVM;


    @Before
    public void setup() throws Exception {
        txService = new TransactionServiceImpl();
        initMocks(this);
        testObj = new VersionServiceImpl();

        testObj.setTxService(txService);
        txService.setVersionService(testObj);

        s = mock(Session.class);
        mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getSession()).thenReturn(s);
        when(mockWorkspace.getName()).thenReturn("default");
        when(s.getWorkspace()).thenReturn(mockWorkspace);
        mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);

        // add a node that's versioned (but not auto-versioned)
        final Node versionedNode = mock(Node.class);
        when(versionedNode.getPath()).thenReturn("/example-versioned");
        when(versionedNode.getSession()).thenReturn(s);
        when(versionedNode.isNodeType(VersionServiceImpl.VERSIONABLE))
                .thenReturn(true);
        when(s.getNode("/example-versioned")).thenReturn(versionedNode);

        // add a node that's autoversioned
        final Node autoversionedNode = mock(Node.class);
        when(autoversionedNode.getPath()).thenReturn("/example-auto-versioned");
        when(autoversionedNode.getSession()).thenReturn(s);
        when(autoversionedNode.isNodeType(VersionServiceImpl.VERSIONABLE))
                .thenReturn(true);
        when(s.getNode("/example-auto-versioned")).thenReturn(autoversionedNode);
        final Property autoVersionProperty = mock(Property.class);
        final Value autoVersionValue = mock(Value.class);
        when(autoVersionValue.getString()).thenReturn(
                VersionServiceImpl.AUTO_VERSION);
        when(autoVersionProperty.isMultiple()).thenReturn(true);
        when(autoVersionProperty.getValues()).thenReturn(new Value[] { autoVersionValue });
        when(autoversionedNode.hasProperty(VersionServiceImpl.VERSION_POLICY))
                .thenReturn(true);
        when(autoversionedNode.getProperty(VersionServiceImpl.VERSION_POLICY))
                .thenReturn(autoVersionProperty);


        // add a node that's unversioned
        final Node unversionedNode = mock(Node.class);
        when(unversionedNode.getPath()).thenReturn("/example-unversioned");
        when(unversionedNode.getSession()).thenReturn(s);
        when(unversionedNode.isNodeType(VersionServiceImpl.VERSIONABLE))
                .thenReturn(false);
        when(s.getNode("/example-unversioned")).thenReturn(unversionedNode);
    }

    @Test
    public void testCheckpoint() throws Exception {
        // request a version be created
        testObj.nodeUpdated(s, "/example-versioned");

        // ensure that it was
        verify(mockVM, never()).checkpoint("/example-versioned");
    }

    @Test
    public void testCheckpointUnversioned() throws Exception {
        // request a version be created
        testObj.nodeUpdated(s, "/example-unversioned");

        // ensure that it was
        verify(mockVM, never()).checkpoint("/example-unversioned");
    }

    @Test
    public void testCheckpointAutoVersioned() throws Exception {
        // request a version be created
        testObj.nodeUpdated(s, "/example-auto-versioned");

        // ensure that it was
        verify(mockVM, only()).checkpoint("/example-auto-versioned");
    }

    @Test
    public void testDeferredCheckpointVersioned() throws Exception {
        // start a transaction
        final Transaction t = txService.beginTransaction(s, USER_NAME);
        s = t.getSession();
        when(s.getNamespaceURI(TransactionServiceImpl.FCREPO4_TX_ID))
                .thenReturn(t.getId());

        assertNotNull("Transaction must have started!",
                txService.getTransaction(
                        s.getNode("/example-auto-versioned").getSession()));

        // request a version be created
        testObj.nodeUpdated(s, "/example-versioned");

        // ensure that no version was created (because the transaction is still open)
        verify(mockVM, never()).checkpoint("/example-versioned");

        // close the transaction
        txService.commit(t.getId());

        // ensure that no version was made because none was explicitly requested
        verify(mockVM, never()).checkpoint("/example-versioned");
    }

    @Test
    public void testDeferredCheckpointUnversioned() throws Exception {
        // start a transaction
        final Transaction t = txService.beginTransaction(s, USER_NAME);
        s = t.getSession();
        when(s.getNamespaceURI(TransactionServiceImpl.FCREPO4_TX_ID))
                .thenReturn(t.getId());

        assertNotNull("Transaction must have started!",
                txService.getTransaction(
                        s.getNode("/example-auto-versioned").getSession()));

        // request a version be created
        testObj.nodeUpdated(s, "/example-unversioned");

        // ensure that no version was created (because the transaction is still open)
        verify(mockVM, never()).checkpoint("/example-unversioned");

        // close the transaction
        txService.commit(t.getId());

        // ensure that no version was made (because versioning is off)
        verify(mockVM, never()).checkpoint("/example-unversioned");
    }

    @Test
    public void testDeferredCheckpointAutoVersioned() throws Exception {
        // start a transaction
        final Transaction t = txService.beginTransaction(s, USER_NAME);
        s = t.getSession();
        when(s.getNamespaceURI(TransactionServiceImpl.FCREPO4_TX_ID))
                .thenReturn(t.getId());

        assertNotNull("Transaction must have started!",
                txService.getTransaction(
                        s.getNode("/example-auto-versioned").getSession()));

        // request a version be created
        testObj.nodeUpdated(s, "/example-auto-versioned");

        // ensure that no version was created (because the transaction is still open)
        verify(mockVM, never()).checkpoint("/example-auto-versioned");

        // close the transaction
        txService.commit(t.getId());

        // ensure that the version was made
        verify(mockVM, only()).checkpoint("/example-auto-versioned");
    }

    @Test
    public void testRevertToVersionByLabel() throws RepositoryException {
        String versionLabel = "v";
        VersionManager mockVersionManager = mock(VersionManager.class);
        VersionHistory mockHistory = mock(VersionHistory.class);
        Version mockVersion1 = mock(Version.class);
        when(mockHistory.getVersionByLabel(versionLabel)).thenReturn(mockVersion1);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example-versioned")).thenReturn(mockHistory);

        testObj.revertToVersion(mockWorkspace, "/example-versioned", versionLabel);
        verify(mockVersionManager).restore(mockVersion1, true);

        verify(mockVersionManager, never()).checkpoint("/example-versioned");
    }

    @Test
    public void testRevertToVersionByUUID() throws RepositoryException {
        String versionUUID = "uuid";
        VersionManager mockVersionManager = mock(VersionManager.class);
        VersionHistory mockHistory = mock(VersionHistory.class);
        Version mockVersion1 = mock(Version.class);
        when(mockHistory.getVersionByLabel(versionUUID)).thenThrow(VersionException.class);
        VersionIterator mockVersionIterator = mock(VersionIterator.class);
        when(mockHistory.getAllVersions()).thenReturn(mockVersionIterator);
        when(mockVersionIterator.hasNext()).thenReturn(true);
        when(mockVersionIterator.nextVersion()).thenReturn(mockVersion1);
        Node mockFrozenNode = mock(Node.class);
        when(mockVersion1.getFrozenNode()).thenReturn(mockFrozenNode);
        when(mockFrozenNode.getIdentifier()).thenReturn(versionUUID);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example-versioned")).thenReturn(mockHistory);

        testObj.revertToVersion(mockWorkspace, "/example-versioned", versionUUID);
        verify(mockVersionManager).restore(mockVersion1, true);
    }

    @Test(expected = PathNotFoundException.class)
    public void testRevertToUnknownVersion() throws RepositoryException {
        String versionUUID = "uuid";
        VersionManager mockVersionManager = mock(VersionManager.class);
        VersionHistory mockHistory = mock(VersionHistory.class);
        Version mockVersion1 = mock(Version.class);
        when(mockHistory.getVersionByLabel(versionUUID)).thenThrow(VersionException.class);
        VersionIterator mockVersionIterator = mock(VersionIterator.class);
        when(mockHistory.getAllVersions()).thenReturn(mockVersionIterator);
        when(mockVersionIterator.hasNext()).thenReturn(false);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example-versioned")).thenReturn(mockHistory);

        testObj.revertToVersion(mockWorkspace, "/example-versioned", versionUUID);
    }

    @Test
    public void testRevertToVersionByLabelWithAutoVersioning() throws RepositoryException {
        String versionLabel = "v";
        VersionManager mockVersionManager = mock(VersionManager.class);
        VersionHistory mockHistory = mock(VersionHistory.class);
        Version mockVersion1 = mock(Version.class);
        when(mockHistory.getVersionByLabel(versionLabel)).thenReturn(mockVersion1);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example-auto-versioned")).thenReturn(mockHistory);

        testObj.revertToVersion(mockWorkspace, "/example-auto-versioned", versionLabel);
        verify(mockVersionManager).restore(mockVersion1, true);
        verify(mockVersionManager).checkpoint("/example-auto-versioned");
    }

    @Test
    public void testRemoveVersionByLabel() throws RepositoryException {
        String versionLabel = "versionName";
        String versionUUID = "uuid";
        String versionName = "Bob";
        String[] versionLabels = new String[]{ versionLabel };
        VersionManager mockVersionManager = mock(VersionManager.class);
        VersionHistory mockHistory = mock(VersionHistory.class);
        Version mockVersion1 = mock(Version.class);
        Version mockVersion2 = mock(Version.class);
        when(mockVersion1.getContainingHistory()).thenReturn(mockHistory);
        when(mockHistory.hasVersionLabel(versionLabel)).thenReturn(true);
        when(mockHistory.getVersionByLabel(versionLabel)).thenReturn(mockVersion1);
        when(mockHistory.getVersionLabels(mockVersion1)).thenReturn(versionLabels);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example")).thenReturn(mockHistory);
        when(mockVersionManager.getBaseVersion("/example")).thenReturn(mockVersion2);
        Node mockFrozenNode = mock(Node.class);
        when(mockVersion1.getFrozenNode()).thenReturn(mockFrozenNode);
        when(mockFrozenNode.getIdentifier()).thenReturn(versionUUID);
        when(mockVersion1.getIdentifier()).thenReturn(versionUUID);
        when(mockVersion1.getName()).thenReturn(versionName);

        testObj.removeVersion(mockWorkspace, "/example", versionLabel);
        verify(mockHistory).removeVersion(versionName);
        verify(mockHistory).removeVersionLabel(versionLabel);
        verify(mockVersionManager, never()).checkpoint("/example");
    }

    @Test
    public void testRemoveVersionByUUID() throws RepositoryException {
        String versionName = "Bob";
        String versionUUID = "uuid";
        String[] versionLabels = new String[]{ };
        VersionManager mockVersionManager = mock(VersionManager.class);
        VersionHistory mockHistory = mock(VersionHistory.class);
        Version mockVersion1 = mock(Version.class);
        Version mockVersion2 = mock(Version.class);
        when(mockVersion1.getContainingHistory()).thenReturn(mockHistory);
        when(mockHistory.getVersionByLabel(versionUUID)).thenThrow(VersionException.class);
        VersionIterator mockVersionIterator = mock(VersionIterator.class);
        when(mockHistory.getAllVersions()).thenReturn(mockVersionIterator);
        when(mockHistory.getVersionLabels(mockVersion1)).thenReturn(versionLabels);
        when(mockVersionIterator.hasNext()).thenReturn(true);
        when(mockVersionIterator.nextVersion()).thenReturn(mockVersion1);
        Node mockFrozenNode = mock(Node.class);
        when(mockVersion1.getFrozenNode()).thenReturn(mockFrozenNode);
        when(mockFrozenNode.getIdentifier()).thenReturn(versionUUID);
        when(mockVersion1.getIdentifier()).thenReturn(versionUUID);
        when(mockVersion1.getName()).thenReturn(versionName);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example")).thenReturn(mockHistory);
        when(mockVersionManager.getBaseVersion("/example")).thenReturn(mockVersion2);

        testObj.removeVersion(mockWorkspace, "/example", versionUUID);
        verify(mockHistory).removeVersion(versionName);
    }

    @Test(expected = PathNotFoundException.class)
    public void testRemoveUnknownVersion() throws RepositoryException {
        String versionUUID = "uuid";
        VersionManager mockVersionManager = mock(VersionManager.class);
        VersionHistory mockHistory = mock(VersionHistory.class);
        Version mockVersion = mock(Version.class);
        when(mockHistory.getVersionByLabel(versionUUID)).thenThrow(VersionException.class);
        VersionIterator mockVersionIterator = mock(VersionIterator.class);
        when(mockHistory.getAllVersions()).thenReturn(mockVersionIterator);
        when(mockVersionIterator.hasNext()).thenReturn(false);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVersionManager);
        when(mockVersionManager.getVersionHistory("/example")).thenReturn(mockHistory);

        testObj.removeVersion(mockWorkspace, "/example", versionUUID);
    }

}
