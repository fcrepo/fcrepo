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

import javax.jcr.Session;
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

    @Before
    public void setup() throws Exception {
        txService = new TransactionService();
        initMocks(this);
        testObj = new VersionService();
        testObj.txService = txService;
    }

    @Test
    public void testCheckpoint() throws Exception {
        Session s = mock(Session.class);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(s.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);

        // request a version be created
        testObj.checkpoint(s, "/example");

        // ensure that it was
        verify(mockVM, only()).checkpoint("/example");
    }

    @Test
    public void testDeferredCheckpoint() throws Exception {
        Session s = mock(Session.class);
        final Workspace mockWorkspace = mock(Workspace.class);
        when(mockWorkspace.getName()).thenReturn("default");
        when(s.getWorkspace()).thenReturn(mockWorkspace);
        final VersionManager mockVM = mock(VersionManager.class);
        when(mockWorkspace.getVersionManager()).thenReturn(mockVM);

        // start a transaction
        Transaction t = txService.beginTransaction(s);
        s = t.getSession();

        // request a version be created
        testObj.checkpoint(s, "/example");

        // ensure that no version was created (because the transaction is still open)
        verify(mockVM, never()).checkpoint("/example");

        // close the transaction
        txService.commit(t.getId());

        // ensure that the version was made
        verify(mockVM, only()).checkpoint("/example");
    }
}
