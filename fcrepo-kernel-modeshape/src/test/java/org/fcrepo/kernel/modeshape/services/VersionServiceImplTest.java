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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.services.VersionService;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test(expected = RepositoryRuntimeException.class)
    @Ignore("Until implemented with Memento")
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
}
