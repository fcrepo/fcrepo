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
package org.fcrepo.integration.kernel.modeshape.services;

import static com.google.common.io.Files.createTempDir;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collection;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.RepositoryService;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>RepositoryServiceImplIT class.</p>
 *
 * @author ksclarke
 * @author ajs6f
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class RepositoryServiceImplIT extends AbstractIT {

    @Inject
    private FedoraRepository repository;

    @Inject
    private RepositoryService repositoryService;

    @Inject
    private BinaryService binaryService;

    @Test
    public void testGetAllObjectsDatastreamSize() throws RepositoryException, InvalidChecksumException {
        final long originalSize;
        FedoraSession session = repository.login();
        try {
            originalSize = repositoryService.getRepositorySize();
            binaryService.findOrCreate(session, "/testObjectServiceNode").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );
            session.commit();
        } finally {
            session.expire();
        }
        try {
            session = repository.login();
            final long afterSize = repositoryService.getRepositorySize();
            assertEquals(4L, afterSize - originalSize);
        } finally {
            session.expire();
        }
    }

    @Test
    public void testBackupRepository() throws Exception {
        final FedoraSession session = repository.login();
        try {
            binaryService.findOrCreate(session, "/testObjectServiceNode0").setContent(
                    new ByteArrayInputStream("asdfx".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );
            session.commit();
            final File backupDirectory = createTempDir();
            final Collection<Throwable> problems = repositoryService.backupRepository(session, backupDirectory);
            assertTrue(problems.isEmpty());
        } finally {
            session.expire();
        }
    }

    @Test
    public void testRestoreRepository() throws Exception {
        final FedoraSession session = repository.login();
        try {
            binaryService.findOrCreate(session, "/testObjectServiceNode1").setContent(
                    new ByteArrayInputStream("asdfy".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );

            session.commit();
            final File backupDirectory = createTempDir();
            repositoryService.backupRepository(session, backupDirectory);
            final Collection<Throwable> problems = repositoryService.restoreRepository(session, backupDirectory);
            assertTrue(problems.isEmpty());
        } finally {
            session.expire();
        }
    }
}
