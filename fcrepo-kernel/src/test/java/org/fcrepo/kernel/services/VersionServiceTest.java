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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.version.VersionManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Mike Durbin
 */
public class VersionServiceTest {


    private VersionService testObj;

    private TransactionService txService;

    @Mock
    private Session s;

    @Mock
    private VersionManager mockVM;

    @Before
    public void setup() throws Exception {
        txService = new TransactionService();
        initMocks(this);
        testObj = new VersionService();
        testObj.txService = txService;
        txService.versionService = testObj;

        s = mock(Session.class);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(s.getWorkspace()).thenReturn(mockWorkspace);
        mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);

        // add a node that's versioned (but not auto-versioned)
        Node versionedNode = mock(Node.class);
        when(versionedNode.isNodeType(VersionService.VERSIONABLE)).thenReturn(true);
        when(s.getNode("/example-versioned")).thenReturn(versionedNode);

        // add a node that's autoversioned
        Node autoversionedNode = mock(Node.class);
        when(autoversionedNode.isNodeType(VersionService.VERSIONABLE)).thenReturn(true);
        when(s.getNode("/example-auto-versioned")).thenReturn(autoversionedNode);
        Property autoVersionProperty = mock(Property.class);
        Value autoVersionValue = mock(Value.class);
        when(autoVersionValue.getString()).thenReturn(VersionService.AUTO_VERSION);
        when(autoVersionProperty.getValues()).thenReturn(new Value[] { autoVersionValue });
        when(autoversionedNode.hasProperty(VersionService.VERSION_POLICY)).thenReturn(true);
        when(autoversionedNode.getProperty(VersionService.VERSION_POLICY)).thenReturn(autoVersionProperty);


        // add a node that's unversioned
        Node unversionedNode = mock(Node.class);
        when(unversionedNode.isNodeType(VersionService.VERSIONABLE)).thenReturn(false);
        when(s.getNode("/example-unversioned")).thenReturn(unversionedNode);
    }

    private void markAsAutoVersioned(Node versionedNode) throws RepositoryException {
        Property autoVersionProperty = mock(Property.class);
        Value autoVersionValue = mock(Value.class);
        when(autoVersionValue.getString()).thenReturn(VersionService.AUTO_VERSION);
        when(autoVersionProperty.getValue()).thenReturn(autoVersionValue);
        when(versionedNode.getProperty(VersionService.VERSION_POLICY)).thenReturn(autoVersionProperty);
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
        Transaction t = txService.beginTransaction(s);
        s = t.getSession();

        // request a version be created
        testObj.nodeUpdated(s, "/example-versioned");

        // ensure that no version was created (because the transaction is still open)
        verify(mockVM, never()).checkpoint("/example-versioned");

        // close the transaction
        txService.commit(t.getId());

        // ensure that the version was made
        verify(mockVM, never()).checkpoint("/example-versioned");
    }

    @Test
    public void testDeferredCheckpointUnversioned() throws Exception {
        // start a transaction
        Transaction t = txService.beginTransaction(s);
        s = t.getSession();

        // request a version be created
        testObj.nodeUpdated(s, "/example-unversioned");

        // ensure that no version was created (because the transaction is still open)
        verify(mockVM, never()).checkpoint("/example-unversioned");

        // close the transaction
        txService.commit(t.getId());

        // ensure that the version was made
        verify(mockVM, never()).checkpoint("/example-unversioned");
    }

    @Test
    public void testDeferredCheckpointAutoVersioned() throws Exception {
        // start a transaction
        Transaction t = txService.beginTransaction(s);
        s = t.getSession();

        // request a version be created
        testObj.nodeUpdated(s, "/example-auto-versioned");

        // ensure that no version was created (because the transaction is still open)
        verify(mockVM, never()).checkpoint("/example-auto-versioned");

        // close the transaction
        txService.commit(t.getId());

        // ensure that the version was made
        verify(mockVM, only()).checkpoint("/example-auto-versioned");
    }
}
