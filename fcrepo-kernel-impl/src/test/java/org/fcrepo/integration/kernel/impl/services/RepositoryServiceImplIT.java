/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.kernel.impl.services;

import static com.google.common.io.Files.createTempDir;
import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.fcrepo.integration.kernel.impl.AbstractIT;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.RepositoryService;
import org.junit.Test;
import org.modeshape.jcr.api.Problems;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>RepositoryServiceImplIT class.</p>
 *
 * @author ksclarke
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class RepositoryServiceImplIT extends AbstractIT {

    @Inject
    private Repository repository;

    @Inject
    RepositoryService repositoryService;

    @Inject
    BinaryService binaryService;

    @Test
    public void testGetAllObjectsDatastreamSize() throws Exception {
        Session session = repository.login();

        final double originalSize = repositoryService.getRepositorySize();


        binaryService.findOrCreate(session, "/testObjectServiceNode").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.save();
        session.logout();

        session = repository.login();

        final double afterSize = repositoryService.getRepositorySize();

        assertEquals(4.0, afterSize - originalSize);

        session.logout();
    }

    @Test
    public void testBackupRepository() throws Exception {
        final Session session = repository.login();

        binaryService.findOrCreate(session, "/testObjectServiceNode0").setContent(
                new ByteArrayInputStream("asdfx".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.save();

        final File backupDirectory = createTempDir();

        final Problems problems = repositoryService.backupRepository(session, backupDirectory);

        assertFalse(problems.hasProblems());
        session.logout();
    }

    @Test
    public void testRestoreRepository() throws Exception {
        final Session session = repository.login();

        binaryService.findOrCreate(session, "/testObjectServiceNode1").setContent(
                new ByteArrayInputStream("asdfy".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.save();

        final File backupDirectory = createTempDir();

        repositoryService.backupRepository(session, backupDirectory);

        final Problems problems = repositoryService.restoreRepository(session, backupDirectory);

        assertFalse(problems.hasProblems());
        session.logout();
    }
}
