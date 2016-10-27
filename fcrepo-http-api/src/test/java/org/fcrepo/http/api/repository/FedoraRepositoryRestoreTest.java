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
package org.fcrepo.http.api.repository;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.services.RepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author Andrew Woods Date: 9/4/13
 */
@Deprecated
public class FedoraRepositoryRestoreTest {

    private FedoraRepositoryRestore repoRestore;

    @Mock
    private RepositoryService mockService;

    @Mock
    private HttpSession mockSession;

    @Mock
    private FedoraSession mockFedoraSession;

    @Before
    public void setUp() {
        initMocks(this);

        repoRestore = new FedoraRepositoryRestore();
        setField(repoRestore, "session", mockSession);
        setField(repoRestore, "repositoryService", mockService);
        setField(repoRestore, "uriInfo", getUriInfoImpl());
        when(mockSession.getFedoraSession()).thenReturn(mockFedoraSession);
    }

    @Test
    public void testRunBackup() throws Exception {
        final Collection<Throwable> problems = new ArrayList<>();
        when(mockService.backupRepository(any(FedoraSession.class), any(File.class)))
                .thenReturn(problems);

        boolean thrown = false;
        try {
            repoRestore.runRestore(null);
            fail("Exception expected");
        } catch (final WebApplicationException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testRunBackupWithDir() throws Exception {
        final Collection<Throwable> problems = new ArrayList<>();
        when(mockService.restoreRepository(any(FedoraSession.class), any(File.class)))
                .thenReturn(problems);

        final String tmpDir = System.getProperty("java.io.tmpdir");
        final InputStream inputStream = new ByteArrayInputStream(tmpDir.getBytes());

        final Response response = repoRestore.runRestore(inputStream);
        assertNotNull(response);
        assertEquals(204, response.getStatus());
    }

}
