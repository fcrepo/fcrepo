/**
 * Copyright 2015 DuraSpace, Inc.
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

import static java.lang.System.getProperty;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import javax.jcr.Session;

import org.fcrepo.kernel.api.services.RepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.Problems;

/**
 * @author Andrew Woods
 *         Date: 9/4/13
 */
public class FedoraRepositoryBackupTest {

    private FedoraRepositoryBackup repoBackup;

    @Mock
    private RepositoryService mockService;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);

        repoBackup = new FedoraRepositoryBackup();
        setField(repoBackup, "session", mockSession);
        setField(repoBackup, "repositoryService", mockService);
        setField(repoBackup, "uriInfo", getUriInfoImpl());
    }

    @Test
    public void testRunBackup() throws Exception {
        final Problems mockProblems = mock(Problems.class);
        when(mockProblems.hasProblems()).thenReturn(false);
        when(mockService.backupRepository(any(Session.class),
                                        any(File.class))).thenReturn(
                mockProblems);

        final String backupPath = repoBackup.runBackup(null);
        assertNotNull(backupPath);
    }

    @Test
    public void testRunBackupWithDir() throws Exception {
        final Problems mockProblems = mock(Problems.class);
        when(mockProblems.hasProblems()).thenReturn(false);
        when(mockService.backupRepository(any(Session.class),
                                        any(File.class))).thenReturn(
                mockProblems);

        final String tmpDir = getProperty("java.io.tmpdir");
        final String tmpDirPath = new File(tmpDir).getCanonicalPath();
        final InputStream inputStream = new ByteArrayInputStream(tmpDir.getBytes());

        final String backupPath = repoBackup.runBackup(inputStream);
        assertNotNull(backupPath);
        assertEquals(tmpDirPath, backupPath);
    }

}
