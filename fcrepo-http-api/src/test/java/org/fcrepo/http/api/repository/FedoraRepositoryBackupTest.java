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
package org.fcrepo.http.api.repository;

import org.fcrepo.kernel.services.NodeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modeshape.jcr.api.Problems;

import javax.jcr.Session;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Andrew Woods
 *         Date: 9/4/13
 */
public class FedoraRepositoryBackupTest {

    private FedoraRepositoryBackup repoBackup;

    @Mock
    private NodeService mockNodes;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        repoBackup = new FedoraRepositoryBackup();
        setField(repoBackup, "session", mockSession);
        setField(repoBackup, "nodeService", mockNodes);
        setField(repoBackup, "uriInfo", getUriInfoImpl());
    }

    @Test
    public void testRunBackup() throws Exception {
        Problems mockProblems = Mockito.mock(Problems.class);
        when(mockProblems.hasProblems()).thenReturn(false);
        when(mockNodes.backupRepository(any(Session.class),
                                        any(File.class))).thenReturn(
                mockProblems);

        String backupPath = repoBackup.runBackup(null);
        assertNotNull(backupPath);
    }

    @Test
    public void testRunBackupWithDir() throws Exception {
        Problems mockProblems = Mockito.mock(Problems.class);
        when(mockProblems.hasProblems()).thenReturn(false);
        when(mockNodes.backupRepository(any(Session.class),
                                        any(File.class))).thenReturn(
                mockProblems);

        String tmpDir = System.getProperty("java.io.tmpdir");
        InputStream inputStream = new ByteArrayInputStream(tmpDir.getBytes());

        String backupPath = repoBackup.runBackup(inputStream);
        assertNotNull(backupPath);
        assertEquals(tmpDir, backupPath);
    }

}
